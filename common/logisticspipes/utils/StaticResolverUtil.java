package logisticspipes.utils;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import org.objectweb.asm.ClassReader;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;

/**
 * Resolves concrete subclasses of a given type by reading the superclass of every class file in
 * the LP mod's root paths (jar or dev output dirs) with ASM, without class-loading the whole mod.
 *
 * <p>The index is built lazily on first call and cached. Abstract classes and inner types that
 * cannot be loaded (e.g. referencing missing optional-mod classes) are silently skipped.
 */
public class StaticResolverUtil {

	@Nullable
	private static Map<String, Set<String>> childrenByParent;

	private StaticResolverUtil() { }

	/** Kept for legacy call sites from the 1.12.2 ASMDataTable era; scanning is lazy now. */
	public static void useASMDataTable(@Nullable Object ignored) {
		// no-op
	}

	private static synchronized void buildIndex() {
		if (childrenByParent != null) {
			return;
		}
		Map<String, Set<String>> map = new HashMap<>();
		try {
			ModContainer mod = FabricLoader.getInstance().getModContainer(LPConstants.LP_MOD_ID).orElse(null);
			if (mod == null) {
				LogisticsPipes.log.error("StaticResolverUtil: LP mod container not found; class resolution disabled");
				childrenByParent = Collections.emptyMap();
				return;
			}
			for (Path root : mod.getRootPaths()) {
				try (Stream<Path> files = Files.walk(root)) {
					for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".class"))::iterator) {
						ClassReader reader = new ClassReader(Files.readAllBytes(file));
						String parent = reader.getSuperName();
						if (parent != null) {
							map.computeIfAbsent(parent.replace('/', '.'), k -> new HashSet<>()).add(reader.getClassName().replace('/', '.'));
						}
					}
				}
			}
		} catch (Throwable t) {
			LogisticsPipes.log.error("StaticResolverUtil: failed to build scan index", t);
			childrenByParent = Collections.emptyMap();
			return;
		}
		childrenByParent = map;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> Set<Class<? extends T>> findClassesByType(@Nonnull Class<T> cls) {
		buildIndex();
		Map<String, Set<String>> index = childrenByParent;
		if (index == null || index.isEmpty()) {
			return Collections.emptySet();
		}

		Set<Class<? extends T>> result = new HashSet<>();
		Deque<String> stack = new ArrayDeque<>();
		Set<String> visited = new HashSet<>();
		stack.push(cls.getName());

		while (!stack.isEmpty()) {
			String current = stack.pop();
			Set<String> kids = index.get(current);
			if (kids == null) {
				continue;
			}
			for (String kid : kids) {
				if (!visited.add(kid)) {
					continue;
				}
				// Continue walking regardless of load success so multi-level
				// hierarchies (A → B → C) are discovered even if B fails to load.
				stack.push(kid);
				Class<?> loaded = loadClass(kid);
				if (loaded == null) {
					continue;
				}
				if (Modifier.isAbstract(loaded.getModifiers()) || loaded.isInterface()) {
					continue;
				}
				if (cls.isAssignableFrom(loaded)) {
					result.add((Class<? extends T>) loaded);
				}
			}
		}
		return result;
	}

	@Nullable
	private static Class<?> loadClass(@Nonnull String classPathSpec) {
		try {
			return Class.forName(classPathSpec, false, LogisticsPipes.class.getClassLoader());
		} catch (Throwable t) {
			// Missing optional-mod dependency or other link error — skip.
			return null;
		}
	}
}
