package com.adobe.marketing.mobile.util

import java.util.Objects
import org.junit.Assert.fail
import java.util.regex.Pattern

/**
 * An interface that defines a multi-path configuration.
 *
 * This interface provides the necessary properties to configure multiple paths
 * within a node configuration context. It is designed to be used where multiple
 * paths need to be specified along with associated configuration options.
 */
interface MultiPathConfig {
    /**
     * An array of optional strings representing the paths to be configured.
     * Each string in the array represents a distinct path. `null` indicates the top-level object.
     */
    val paths: List<String?>

    /**
     * A `NodeConfig.OptionKey` value that specifies the type of option applied to the paths.
     */
    val optionKey: NodeConfig.OptionKey

    /**
     * A Boolean value indicating whether the configuration is active.
     */
    val config: NodeConfig.Config

    /**
     * A `NodeConfig.Scope` value defining the scope of the configuration, such as whether it is applied to a single node or a subtree.
     */
    val scope: NodeConfig.Scope
}

/**
 * A data class representing the configuration for a single path.
 *
 * This data class is used to define the configuration details for a specific path within
 * a node configuration context. It encapsulates the path's specific options and settings.
 */
data class PathConfig(
    /**
     * An optional String representing the path to be configured. `null` indicates the top-level object.
     */
    var path: String?,

    /**
     * A `NodeConfig.OptionKey` value that specifies the type of option applied to the path.
     */
    var optionKey: NodeConfig.OptionKey,

    /**
     * A Boolean value indicating whether the configuration is active.
     */
    var config: NodeConfig.Config,

    /**
     * A `NodeConfig.Scope` value defining the scope of the configuration, such as whether it is applied to a single node or a subtree.
     */
    var scope: NodeConfig.Scope
)

/**
 * Validation option which specifies: Array elements from `expected` may match elements from `actual` regardless of index position.
 * When combining any position option indexes and standard indexes, standard indexes are validated first.
 */
data class AnyOrderMatch(
    override val paths: List<String?> = listOf(null),
    override val optionKey: NodeConfig.OptionKey = NodeConfig.OptionKey.AnyOrderMatch,
    override val config: NodeConfig.Config = NodeConfig.Config(isActive = true),
    override val scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode
) : MultiPathConfig {

    /**
     * Initializes a new instance with an array of paths.
     *
     * @param paths An array of optional path strings.
     * @param isActive Boolean value indicating whether the configuration is active.
     * @param scope The scope of configuration, defaulting to single node.
     */
    @JvmOverloads
    constructor(paths: List<String?> = listOf(null), isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(paths, config = NodeConfig.Config(isActive = isActive), scope = scope)

    /**
     * Variadic initializer allowing multiple string paths.
     *
     * @param paths Vararg of optional path strings.
     * @param isActive Boolean value indicating whether the configuration is active.
     * @param scope The scope of configuration, defaulting to single node.
     */
    @JvmOverloads
    constructor(vararg paths: String?, isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(
                if (paths.isEmpty()) listOf(null) else paths.toList(),
                isActive = isActive,
                scope = scope
            )
}

/**
 * Validation option which specifies: Collections (objects and/or arrays) must have the same number of elements.
 */
data class CollectionEqualCount(
    override val paths: List<String?> = listOf(null),
    override val optionKey: NodeConfig.OptionKey = NodeConfig.OptionKey.CollectionEqualCount,
    override val config: NodeConfig.Config = NodeConfig.Config(isActive = true),
    override val scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode
) : MultiPathConfig {

    /**
     * Secondary constructor for initializing with a list of paths.
     *
     * @param paths A list of optional path strings.
     * @param isActive Boolean value indicating whether the configuration is active.
     * @param scope The scope of configuration, defaulting to single node.
     */
    @JvmOverloads
    constructor(paths: List<String?> = listOf(null), isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(paths, config = NodeConfig.Config(isActive = isActive), scope = scope)

    /**
     * Variadic initializer allowing multiple string paths.
     *
     * @param paths Vararg of optional path strings.
     * @param isActive Specifies whether this configuration is active.
     * @param scope Specifies the scope of the configuration.
     */
    @JvmOverloads
    constructor(vararg paths: String?, isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(
        if (paths.isEmpty()) listOf(null) else paths.toList(),
        isActive = isActive,
        scope = scope
    )
}

/**
 * Validation option which specifies: `actual` must not have the key name specified.
 */
data class KeyMustBeAbsent(
    override val paths: List<String?> = listOf(null),
    override val optionKey: NodeConfig.OptionKey = NodeConfig.OptionKey.KeyMustBeAbsent,
    override val config: NodeConfig.Config = NodeConfig.Config(isActive = true),
    override val scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode
) : MultiPathConfig {

    /**
     * Initializes a new instance with an array of paths.
     *
     * @param paths A list of optional path strings, defaults to a list with a single null if no paths are provided.
     * @param isActive Boolean value indicating whether the configuration is active.
     * @param scope The scope of configuration, defaulting to single node.
     */
    @JvmOverloads
    constructor(paths: List<String?> = listOf(null), isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(paths, NodeConfig.OptionKey.KeyMustBeAbsent, NodeConfig.Config(isActive = isActive), scope)

    /**
     * Variadic initializer allowing multiple string paths.
     *
     * @param paths Vararg of optional path strings. When empty, defaults to a list containing null.
     * @param isActive Specifies whether this configuration is active.
     * @param scope Specifies the scope of the configuration.
     */
    @JvmOverloads
    constructor(vararg paths: String?, isActive: Boolean = true, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(
        if (paths.isEmpty()) listOf(null) else paths.toList(),
        isActive = isActive,
        scope = scope
    )
}

/**
 * Validation option which specifies that values must have the same type and literal value.
 * This class applies to specified paths within a data structure, ensuring that values at these paths
 * are exactly the same both in type and value.
 *
 * @property paths List of optional string paths indicating where the exact match validation is applied.
 * @property optionKey Constant from NodeConfig.OptionKey indicating the specific validation option for exact matches.
 * @property config Configuration details indicating whether this validation is active.
 * @property scope Scope of the validation, indicating the extent of the data structure this rule applies to.
 */
data class ValueExactMatch(
    override val paths: List<String?> = listOf(null),
    override val optionKey: NodeConfig.OptionKey = NodeConfig.OptionKey.PrimitiveExactMatch,
    override val config: NodeConfig.Config = NodeConfig.Config(isActive = true),
    override val scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode
) : MultiPathConfig {

    /**
     * Secondary constructor for initializing with a list of paths.
     *
     * @param paths A list of optional path strings, defaults to a list containing null if no paths are provided.
     * @param scope The scope of configuration, typically single node or subtree.
     */
    @JvmOverloads
    constructor(paths: List<String?> = listOf(null), scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(paths, config = NodeConfig.Config(isActive = true), scope = scope)

    /**
     * Variadic constructor allowing multiple string paths. This constructor handles empty path inputs
     * by creating a list containing a single null element, ensuring consistent validation behavior.
     *
     * @param paths Vararg of optional path strings, defaults to a list containing null when empty.
     * @param scope Specifies the scope of the configuration, typically single node or subtree.
     */
    @JvmOverloads
    constructor(vararg paths: String?, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(
        if (paths.isEmpty()) listOf(null) else paths.toList(),
        scope = scope
    )
}

/**
 * Validation option which specifies: values must have the same type but their literal values can be different.
 */
data class ValueTypeMatch(
    override val paths: List<String?> = listOf(null),
    override val optionKey: NodeConfig.OptionKey = NodeConfig.OptionKey.PrimitiveExactMatch,
    override val config: NodeConfig.Config = NodeConfig.Config(isActive = false),
    override val scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode
) : MultiPathConfig {

    /**
     * Initializes a new instance with an array of paths.
     *
     * @param paths A list of optional path strings, defaults to a list containing null if no paths are provided.
     * @param scope The scope of configuration, typically single node or subtree.
     */
    @JvmOverloads
    constructor(paths: List<String?> = listOf(null), scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(paths, config = NodeConfig.Config(isActive = false), scope = scope)

    /**
     * Variadic initializer allowing multiple string paths.
     *
     * @param paths Vararg of optional path strings, defaults to a list containing null when empty.
     * @param scope Specifies the scope of the configuration, typically single node or subtree.
     */
    @JvmOverloads
    constructor(vararg paths: String?, scope: NodeConfig.Scope = NodeConfig.Scope.SingleNode)
            : this(
        if (paths.isEmpty()) listOf(null) else paths.toList(),
        scope = scope
    )
}

/**
 * A class representing the configuration for a node in a tree structure.
 *
 * `NodeConfig` provides a way to set configuration options for nodes in a hierarchical tree structure.
 * It supports different types of configuration options, including options that apply to individual nodes
 * or to entire subtrees.
 */
class NodeConfig {
    /**
     * Represents the scope of the configuration; that is, to which nodes the configuration applies.
     */
    enum class Scope(val value: String) {
        SingleNode("SingleNode"),
        Subtree("Subtree")
    }

    /**
     * Defines the types of configuration options available for nodes.
     */
    enum class OptionKey(val value: String) {
        AnyOrderMatch("AnyOrderMatch"),
        CollectionEqualCount("CollectionEqualCount"),
        PrimitiveExactMatch("PrimitiveExactMatch"),
        KeyMustBeAbsent("KeyMustBeAbsent")
    }

    /**
     * Represents the configuration details for a comparison option
     */
    data class Config(var isActive: Boolean)

    data class NodeOption(
        val optionKey: OptionKey,
        val config: Config,
        val scope: Scope
    )

    private data class PathComponent(
        var name: String?,
        var isAnyOrder: Boolean,
        var isArray: Boolean,
        var isWildcard: Boolean
    )

    /**
     * A string representing the name of the node. `null` refers to the top level object
     */
    var name: String? = null
    /**
     * Options set specifically for this node. Specific `OptionKey`s may or may not be present - it is optional.
     */
    var options: MutableMap<OptionKey, Config> = mutableMapOf()
    /**
     * Options set for the subtree, used as the default option when no node-specific options are set. All `OptionKey`s MUST be
     * present.
     */
    private var subtreeOptions: MutableMap<OptionKey, Config> = mutableMapOf()

    /**
     * The set of child nodes.
     */
    private var children: MutableSet<NodeConfig> = mutableSetOf()
    /**
     * The node configuration for wildcard children
     */
    private var wildcardChildren: NodeConfig? = null

    // Property accessors for each option which use the `options` set for the current node
    // and fall back to subtree options.
    var anyOrderMatch: Config
        get() = options[OptionKey.AnyOrderMatch] ?: subtreeOptions[OptionKey.AnyOrderMatch]!!
        set(value) { options[OptionKey.AnyOrderMatch] = value }

    var collectionEqualCount: Config
        get() = options[OptionKey.CollectionEqualCount] ?: subtreeOptions[OptionKey.CollectionEqualCount]!!
        set(value) { options[OptionKey.CollectionEqualCount] = value }

    var keyMustBeAbsent: Config
        get() = options[OptionKey.KeyMustBeAbsent] ?: subtreeOptions[OptionKey.KeyMustBeAbsent]!!
        set(value) { options[OptionKey.KeyMustBeAbsent] = value }

    var primitiveExactMatch: Config
        get() = options[OptionKey.PrimitiveExactMatch] ?: subtreeOptions[OptionKey.PrimitiveExactMatch]!!
        set(value) { options[OptionKey.PrimitiveExactMatch] = value }

    /**
     * Creates a new node with the given values.
     *
     * Make sure to specify **all** `OptionKey` values for `subtreeOptions`, especially when the node is intended to be the root.
     * These subtree options will be used for all descendants unless otherwise specified. If any subtree option keys are missing,
     * a default value will be provided.
     */
    @JvmOverloads
    constructor(name: String?,
                options: MutableMap<OptionKey, Config> = mutableMapOf(),
                subtreeOptions: MutableMap<OptionKey, Config>,
                children: MutableSet<NodeConfig> = mutableSetOf(),
                wildcardChildren: NodeConfig? = null) {
        // Validate subtreeOptions has every option defined
        val validatedSubtreeOptions = subtreeOptions.toMutableMap()
        OptionKey.values().forEach { key ->
            if (!validatedSubtreeOptions.containsKey(key)) {
                validatedSubtreeOptions[key] = Config(isActive = false)
            }
        }

        this.name = name
        this.options = options
        this.subtreeOptions = validatedSubtreeOptions
        this.children = children
        this.wildcardChildren = wildcardChildren
    }

    companion object {
        /**
         * Resolves a given node's option using the following precedence:
         * 1. Single node option
         *    a. Current node
         *    b. Wildcard child
         *    c. Parent node
         *
         * 2. Subtree option
         *    a. Current node
         *    b. Wildcard child
         *    c. Parent node
         *
         * This is to handle the case where an array has a node-specific option like wildcard match which
         * should apply to all direct children (that is, only 1 level down), and one of the children has a
         * node specific option disabling wildcard match.
         */
        @JvmStatic
        fun resolveOption(option: OptionKey, node: NodeConfig?, parentNode: NodeConfig): Config {
            // Single node options
            // Current node
            node?.options?.get(option)?.let {
                return it
            }
            // Wildcard child
            node?.wildcardChildren?.options?.get(option)?.let {
                return it
            }
            // Check array's node-specific option
            parentNode.options[option]?.let {
                return it
            }
            // Check node's subtree option, falling back to array node's default subtree config
            return when (option) {
                OptionKey.AnyOrderMatch ->
                    node?.anyOrderMatch ?: node?.wildcardChildren?.anyOrderMatch ?: parentNode.anyOrderMatch
                OptionKey.CollectionEqualCount ->
                    node?.collectionEqualCount ?: node?.wildcardChildren?.collectionEqualCount ?: parentNode.collectionEqualCount
                OptionKey.KeyMustBeAbsent ->
                    node?.keyMustBeAbsent ?: node?.wildcardChildren?.keyMustBeAbsent ?: parentNode.keyMustBeAbsent
                OptionKey.PrimitiveExactMatch ->
                    node?.primitiveExactMatch ?: node?.wildcardChildren?.primitiveExactMatch ?: parentNode.primitiveExactMatch
            }
        }
    }

    /**
     * Determines if two `NodeConfig` instances are equal based on their properties.
     */
    override fun equals(other: Any?): Boolean = other is NodeConfig &&
            name == other.name &&
            options == other.options &&
            subtreeOptions == other.subtreeOptions

    /**
     * Generates a hash code for a `NodeConfig`.
     */
    override fun hashCode(): Int = Objects.hash(name, options, subtreeOptions)

    /**
     * Creates a deep copy of the current `NodeConfig` instance.
     */
    fun deepCopy(): NodeConfig {
        return NodeConfig(
            name = name,
            options = HashMap(options),
            subtreeOptions = HashMap(subtreeOptions),
            children = children.map { it.deepCopy() }.toMutableSet(),
            wildcardChildren = wildcardChildren?.deepCopy()
        )
    }

    /**
     * Gets a child node with the specified name.
     */
    fun getChild(name: String?): NodeConfig? = children.firstOrNull { it.name == name }

    /**
     * Gets a child node at the specified index if it represents as a string.
     */
    fun getChild(index: Int?): NodeConfig? {
        return index?.let {
            val indexString = it.toString()
            children.firstOrNull { child -> child.name == indexString }
        }
    }

    /**
     * Gets the next node for the given name, falling back to wildcard or asFinalNode if not found.
     */
    fun getNextNode(forName: String?): NodeConfig =
        getChild(forName) ?: wildcardChildren ?: asFinalNode()

    /**
     * Gets the next node for the given index, falling back to wildcard or asFinalNode if not found.
     */
    fun getNextNode(forIndex: Int?): NodeConfig =
        getChild(forIndex) ?: wildcardChildren ?: asFinalNode()

    /**
     * Creates a new NodeConfig instance representing the final node configuration.
     * This function is used to create a snapshot of the current node configuration,
     * ensuring that modifications to the new instance do not affect the original node's state,
     * particularly useful in recursive or multi-threaded environments.
     *
     * @return A new NodeConfig instance with the current subtree options.
     */
    fun asFinalNode(): NodeConfig {
        // Should not modify self since other recursive function calls may still depend on children.
        // Instead, return a new instance with the proper values set
        return NodeConfig(name = null, options = mutableMapOf(), subtreeOptions = subtreeOptions)
    }

    /**
     * Creates or updates nodes based on multiple path configurations.
     * This function processes a collection of paths and updates or creates the corresponding nodes.
     *
     * @param multiPathConfig Configuration for multiple paths including common option key, config, and scope.
     * @param isLegacyMode Flag indicating if the operation should consider legacy behaviors.
     */
    fun createOrUpdateNode(multiPathConfig: MultiPathConfig, isLegacyMode: Boolean) {
        val pathConfigs = multiPathConfig.paths.map {
            PathConfig(
                path = it,
                optionKey = multiPathConfig.optionKey,
                config = multiPathConfig.config,
                scope = multiPathConfig.scope
            )
        }
        for (pathConfig in pathConfigs) {
            createOrUpdateNode(pathConfig, isLegacyMode)
        }
    }

    /**
     * Helper method to create or traverse nodes.
     * This function processes a single path configuration and updates or creates nodes accordingly.
     *
     * @param pathConfig Configuration for a single path including option key, config, and scope.
     * @param isLegacyMode Flag indicating if the operation should consider legacy behaviors.
     */
    fun createOrUpdateNode(pathConfig: PathConfig, isLegacyMode: Boolean) {
        val pathComponents = getProcessedPathComponents(pathConfig.path)
        updateTree(nodes = mutableListOf(this), pathConfig = pathConfig, pathComponents = pathComponents, isLegacyMode = isLegacyMode)
    }

    /**
     * Updates a tree of nodes based on the provided path configuration and path components.
     * This function recursively applies configurations to nodes, traversing through the path defined by the path components.
     * It supports applying options to individual nodes or entire subtrees based on the scope defined in the path configuration.
     *
     * @param nodes The list of current nodes to update.
     * @param pathConfig The configuration to apply, including the option key and its scope.
     * @param pathComponents The components of the path, dictating how deep the configuration should be applied.
     * @param isLegacyMode A flag indicating whether legacy mode is enabled, affecting how certain options are applied.
     */
    private fun updateTree(nodes: MutableList<NodeConfig>, pathConfig: PathConfig, pathComponents: MutableList<PathComponent>, isLegacyMode: Boolean) {
        if (nodes.isEmpty()) return
        // Reached the end of the pathComponents - apply the PathConfig to the current nodes
        if (pathComponents.isEmpty()) {
            // Apply the node option to the final node
            nodes.forEach { node ->
                if (pathConfig.scope == Scope.Subtree) {
                    // Propagate this subtree option update to all children
                    propagateSubtreeOption(node, pathConfig)
                } else {
                    node.options[pathConfig.optionKey] = pathConfig.config
                }
            }
            return
        }

        // Remove the first path component to progress the recursion by 1
        val pathComponent = pathComponents.removeFirst()
        val nextNodes = mutableListOf<NodeConfig>()

        nodes.forEach { node ->
            // Note: the `[*]` case is processed as name = "[*]" not name is nil
            pathComponent.name?.let { pathComponentName ->
                val child = findOrCreateChild(node, pathComponentName, pathComponent.isWildcard)
                nextNodes.add(child)

                if (pathComponent.isWildcard) {
                    nextNodes.addAll(node.children)
                }
                if (isLegacyMode && pathComponent.isAnyOrder) {
                    // This is the legacy AnyOrder that should apply to all children
                    // Apply the option to the parent level so it applies to all children
                    if (pathComponentName == "[*]") {
                        node.options[OptionKey.AnyOrderMatch] =
                            Config(isActive = true)
                    } else {
                        child.options[OptionKey.AnyOrderMatch] =
                            Config(isActive = true)
                    }
                }
            }
        }
        updateTree(nextNodes, pathConfig, pathComponents, isLegacyMode)
    }

    /**
     * Processes the given path string into individual path components with detailed properties.
     * This function analyzes a path string, typically representing a navigation path in a structure,
     * and breaks it down into components that specify details about how each segment of the path should be treated,
     * such as whether it's an array, a wildcard, or requires any specific order handling.
     *
     * @param pathString The path string to be processed.
     * @return A list of [PathComponent] reflecting the structured breakdown of the path string.
     */
    private fun getProcessedPathComponents(pathString: String?): MutableList<PathComponent> {
        val objectPathComponents = getObjectPathComponents(pathString)
        val pathComponents = mutableListOf<PathComponent>()
        for (objectPathComponent in objectPathComponents) {
            val key = objectPathComponent.replace("\\.", ".")
            // Extract the string part and array component part(s) from the key string
            val (stringComponent, arrayComponents) = getArrayPathComponents(key)
            // Process string segment
            stringComponent?.let {
                val isWildcard = stringComponent == "*"
                if (isWildcard) {
                    pathComponents.add(
                        PathComponent(
                            name = stringComponent,
                            isAnyOrder = false,
                            isArray = false,
                            isWildcard = isWildcard
                        )
                    )
                } else {
                    pathComponents.add(
                        PathComponent(
                            name = stringComponent.replace("\\*", "*"),
                            isAnyOrder = false,
                            isArray = false,
                            isWildcard = isWildcard
                        )
                    )
                }
            }

            // Process array segment(s)
            for (arrayComponent in arrayComponents) {
                if (arrayComponent == "[*]") {
                    pathComponents.add(
                        PathComponent(
                            name = arrayComponent,
                            isAnyOrder = true,
                            isArray = true,
                            isWildcard = true
                        )
                    )
                } else {
                    val indexResult = getArrayIndexAndAnyOrder(arrayComponent)
                    indexResult?.let {
                        pathComponents.add(
                            PathComponent(
                                name = it.first.toString(),
                                isAnyOrder = it.second,
                                isArray = true,
                                isWildcard = false
                            )
                        )
                    }
                        ?: return pathComponents // Test failure emitted by extractIndexAndWildcardStatus
                }
            }
        }
        return pathComponents
    }

    /**
     * Finds or creates a child node within the given node, handling the assignment to the proper descendants' location.
     * This method ensures that if the child node already exists, it is returned; otherwise, a new child node is created.
     * If a wildcard child node is needed, it either returns an existing wildcard child or creates a new one and assigns it.
     *
     * @param node The parent node in which to find or create a child.
     * @param name The name of the child node to find or create.
     * @param isWildcard Indicates whether the child node to be created should be treated as a wildcard node.
     * @return The found or newly created child node.
     */
    private fun findOrCreateChild(node: NodeConfig, name: String, isWildcard: Boolean): NodeConfig {
        return if (isWildcard) {
            node.wildcardChildren ?: run {
                // Apply subtreeOptions to the child
                val newChild = NodeConfig(name = name, subtreeOptions = node.subtreeOptions)
                node.wildcardChildren = newChild
                newChild
            }
        } else {
            node.children.firstOrNull { it.name == name } ?: run {
                // If a wildcard child already exists, use that as the base
                node.wildcardChildren?.deepCopy()?.apply {
                    this.name = name
                    node.children.add(this)
                } ?: run {
                    // Apply subtreeOptions to the child
                    val newChild = NodeConfig(name = name, subtreeOptions = node.subtreeOptions)
                    node.children.add(newChild)
                    newChild
                }
            }
        }
    }

    /**
     * Propagates a subtree option from the given path configuration to the specified node and all its descendants.
     * This function recursively ensures that the specified option is applied consistently throughout the subtree
     * originating from the given node.
     *
     * @param node The node from which to start propagating the subtree option.
     * @param pathConfig The configuration containing the option to propagate.
     */
    private fun propagateSubtreeOption(node: NodeConfig, pathConfig: PathConfig) {
        val key = pathConfig.optionKey
        node.subtreeOptions[key] = pathConfig.config
        // Should be impossible for subtree map to be missing keys; fix constructor if this ever results in NPE
        node.wildcardChildren?.subtreeOptions?.set(key, node.subtreeOptions[key]!!)
        for (child in node.children) {
            // Only propagate the subtree value for the current option key,
            // otherwise, previously set subtree values will be reset to the default values
            child.subtreeOptions[key] = node.subtreeOptions[key]!!
            propagateSubtreeOption(child, pathConfig)
        }
    }

    /**
     * Extracts and returns a pair with a valid index and a flag indicating whether it's an `AnyOrder` index from a single array path segment.
     *
     * This method considers a key that matches the array access format (ex: `[*123]` or `[123]`).
     * It identifies an index by optionally checking for the wildcard marker `*`.
     *
     * @param pathComponent A single path component which may contain a potential index with or without a wildcard marker.
     * @return A Pair containing an optional valid `Int` index and a boolean indicating whether it's a wildcard index,
     *   returns `null` if no valid index is found.
     *
     * Note:
     * Examples of conversions:
     * - `[*123]` -> Pair(123, true)
     * - `[123]` -> Pair(123, false)
     * - `[*ab12]` causes a failure since "ab12" is not a valid integer.
     */
    private fun getArrayIndexAndAnyOrder(pathComponent: String): Pair<Int, Boolean>? {
        val arrayIndexValueRegex = "^\\[(.*?)\\]$".toRegex()
        val arrayIndexValue = arrayIndexValueRegex.find(pathComponent)?.groupValues?.get(1)

        if (arrayIndexValue == null) {
            fail("Error: unable to find valid index value from path component: $pathComponent")
            return null
        }

        val isAnyOrder = arrayIndexValue.startsWith("*")
        val indexString = if (isAnyOrder) arrayIndexValue.drop(1) else arrayIndexValue

        val validIndex = indexString.toIntOrNull()
        if (validIndex == null) {
            fail("Error: Index is not a valid Int: $indexString")
            return null
        }

        return Pair(validIndex, isAnyOrder)
    }

    /**
     * Finds all matches of the `regexPattern` in the `text` and for each match, returns the original matched `String`
     * and its corresponding non-null capture groups.
     *
     * @param text The input `String` on which the regex matching is to be performed.
     * @param regexPattern The regex pattern to be used for matching against the `text`.
     * @return A list of pairs, where each pair consists of the original matched `String` and a list of its non-null capture groups.
     *         Returns `null` if an invalid regex pattern is provided.
     */
    private fun extractRegexCaptureGroups(text: String, regexPattern: String): List<Pair<String, List<String>>>? {
        return try {
            val regex = Pattern.compile(regexPattern)
            val matcher = regex.matcher(text)
            val matchResult = mutableListOf<Pair<String, List<String>>>()

            while (matcher.find()) {
                val matchString = text.substring(matcher.start(), matcher.end())
                val captureGroups = (1 until matcher.groupCount() + 1).mapNotNull {
                    if (matcher.start(it) != -1 && matcher.end(it) != -1) text.substring(matcher.start(it), matcher.end(it)) else null
                }
                matchResult.add(Pair(matchString, captureGroups))
            }

            matchResult.takeIf { it.isNotEmpty() }
        } catch (e: IllegalArgumentException) {
            fail("Error: Invalid regex: ${e.message}")
            null
        }
    }

    /**
     * Applies the provided regex pattern to the text and returns all the capture groups from the regex pattern.
     *
     * @param text The input `String` on which the regex matching is to be performed.
     * @param regexPattern The regex pattern to be used for matching against the `text`.
     * @return A list of all capture groups extracted from the regex pattern across all matches.
     */
    private fun getCapturedRegexGroups(text: String, regexPattern: String): List<String> {
        return try {
            val regex = Pattern.compile(regexPattern)
            val matcher = regex.matcher(text)
            val captureGroups = mutableListOf<String>()

            while (matcher.find()) {
                for (i in 1..matcher.groupCount()) {
                    val captured = matcher.group(i)
                    if (captured != null) {
                        captureGroups.add(captured)
                    }
                }
            }
            captureGroups
        } catch (e: IllegalArgumentException) {
            fail("Error: Invalid regex: ${e.message}")
            listOf() // Return an empty list after logging the failure
        }
    }


    /**
     * Breaks a path string into its nested *object* segments. Any trailing *array* style access components are bundled with a
     * preceding object segment (if the object segment exists).
     *
     * For example, the key path: `"key0\.key1.key2[1][2].key3"`, represents a path to an element in a nested
     * JSON structure. The result for the input is: `["key0\.key1", "key2[1][2]", "key3"]`.
     *
     * The method breaks each object path segment separated by the `.` character and escapes
     * the sequence `\.` as a part of the key itself (that is, it ignores `\.` as a nesting indicator).
     *
     * @param path The key path string to be split into its nested object segments.
     * @return A list of strings representing the individual components of the key path. If the input `path` is null or empty,
     * a list containing an empty string is returned. If no components are found, an empty list is returned.
     */
    fun getObjectPathComponents(path: String?): List<String> {
        // Handle edge case where input is null or empty
        if (path.isNullOrEmpty()) return listOf("")

        val segments = mutableListOf<String>()
        var startIndex = 0
        var inEscapeSequence = false

        // Iterate over each character in the input string with its index
        path.forEachIndexed { index, char ->
            when {
                char == '\\' -> inEscapeSequence = true
                char == '.' && !inEscapeSequence -> {
                    // Add the segment from the start index to current index (excluding the dot)
                    segments.add(path.substring(startIndex, index))

                    // Update the start index for the next segment
                    startIndex = index + 1
                }
                else -> inEscapeSequence = false
            }
        }

        // Add the remaining segment after the last dot (if any)
        segments.add(path.substring(startIndex))

        // Handle edge case where input ends with a dot (but not an escaped dot)
        if (path.endsWith(".") && !path.endsWith("\\.") && segments.last().isNotEmpty()) {
            segments.add("")
        }

        return segments
    }

    /**
     * Extracts valid array format access components from a given path component and returns the separated components.
     *
     * Given `"key1[0][1]"`, the result is `["key1", "[0]", "[1]"]`.
     * Array format access can be escaped using a backslash character preceding an array bracket. Valid bracket escape sequences are cleaned so
     * that the final path component does not have the escape character.
     * For example: `"key1\[0\]"` results in the single path component `"key1[0]"`.
     *
     * @param pathComponent The path component to be split into separate components given valid array formatted components.
     * @return A Pair containing the string component of the path, if any, and a list of string path components representing
     * the individual elements of the array accesses, if present.
     */
    fun getArrayPathComponents(pathComponent: String): Pair<String?, List<String>> {
        // Handle edge case where input is empty
        if (pathComponent.isEmpty()) return Pair("", listOf())

        var stringComponent = ""
        val arrayComponents = mutableListOf<String>()
        var bracketCount = 0
        var componentBuilder = StringBuilder()
        var lastArrayAccessEnd = pathComponent.length // to track the end of the last valid array-style access

        fun isNextCharBackslash(index: Int): Boolean {
            if (index == 0) {
                // There is no character before the startIndex.
                return false
            }
            // Since we're iterating in reverse, the "next" character is before i
            return pathComponent[index - 1] == '\\'
        }

        for (index in pathComponent.indices.reversed()) {
            when {
                pathComponent[index] == ']' && !isNextCharBackslash(index) -> {
                    bracketCount += 1
                    componentBuilder.append("]")
                }
                pathComponent[index] == '[' && !isNextCharBackslash(index) -> {
                    bracketCount -= 1
                    componentBuilder.append("[")
                    if (bracketCount == 0) {
                        arrayComponents.add(0, componentBuilder.toString().reversed())
                        componentBuilder.clear()
                        lastArrayAccessEnd = index
                    }
                }
                pathComponent[index] == '\\' -> continue // skip adding the escape character
                bracketCount == 0 && index < lastArrayAccessEnd -> {
                    stringComponent = pathComponent.substring(0, index + 1)
                    break
                }
                else -> componentBuilder.append(pathComponent[index])
            }
        }

        // Add any remaining component that's not yet added
        if (componentBuilder.isNotEmpty()) {
            stringComponent = componentBuilder.toString().reversed()
        }
        if (stringComponent.isNotEmpty()) {
            stringComponent = stringComponent
                .replace("\\[", "[")
                .replace("\\]", "]")
        }

        if (lastArrayAccessEnd == 0) {
            return Pair(null, arrayComponents)
        }
        return Pair(stringComponent, arrayComponents)
    }
}