package com.adobe.marketing.mobile.util

import java.util.Objects

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
    val paths: Array<String?>

    /**
     * A `NodeConfig.OptionKey` value that specifies the type of option applied to the paths.
     */
    val optionKey: NodeConfig.NodeConfig.OptionKey

    /**
     * A Boolean value indicating whether the configuration is active.
     */
    val config: NodeConfig.NodeConfig.Config

    /**
     * A `NodeConfig.Scope` value defining the scope of the configuration, such as whether it is applied to a single node or a subtree.
     */
    val scope: NodeConfig.NodeConfig.Scope
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
    var optionKey: NodeConfig.NodeConfig.OptionKey,

    /**
     * A Boolean value indicating whether the configuration is active.
     */
    var config: NodeConfig.NodeConfig.Config,

    /**
     * A `NodeConfig.Scope` value defining the scope of the configuration, such as whether it is applied to a single node or a subtree.
     */
    var scope: NodeConfig.NodeConfig.Scope
)

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
        singleNode("SingleNode"),
        subtree("Subtree")
    }

    /**
     * Defines the types of configuration options available for nodes.
     */
    enum class OptionKey(val value: String) {
        anyOrderMatch("AnyOrderMatch"),
        collectionEqualCount("CollectionEqualCount"),
        primitiveExactMatch("PrimitiveExactMatch"),
        keyMustBeAbsent("KeyMustBeAbsent")
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
        get() = options[OptionKey.anyOrderMatch] ?: subtreeOptions[OptionKey.anyOrderMatch]!!
        set(value) { options[OptionKey.anyOrderMatch] = value }

    var collectionEqualCount: Config
        get() = options[OptionKey.collectionEqualCount] ?: subtreeOptions[OptionKey.collectionEqualCount]!!
        set(value) { options[OptionKey.collectionEqualCount] = value }

    var keyMustBeAbsent: Config
        get() = options[OptionKey.keyMustBeAbsent] ?: subtreeOptions[OptionKey.keyMustBeAbsent]!!
        set(value) { options[OptionKey.keyMustBeAbsent] = value }

    var primitiveExactMatch: Config
        get() = options[OptionKey.primitiveExactMatch] ?: subtreeOptions[OptionKey.primitiveExactMatch]!!
        set(value) { options[OptionKey.primitiveExactMatch] = value }

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
        fun resolveOption(option: NodeConfig.OptionKey, node: NodeConfig?, parentNode: NodeConfig): NodeConfig.Config {
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
                NodeConfig.OptionKey.anyOrderMatch ->
                    node?.anyOrderMatch ?: node?.wildcardChildren?.anyOrderMatch ?: parentNode.anyOrderMatch
                NodeConfig.OptionKey.collectionEqualCount ->
                    node?.collectionEqualCount ?: node?.wildcardChildren?.collectionEqualCount ?: parentNode.collectionEqualCount
                NodeConfig.OptionKey.keyMustBeAbsent ->
                    node?.keyMustBeAbsent ?: node?.wildcardChildren?.keyMustBeAbsent ?: parentNode.keyMustBeAbsent
                NodeConfig.OptionKey.primitiveExactMatch ->
                    node?.primitiveExactMatch ?: node?.wildcardChildren?.primitiveExactMatch ?: parentNode.primitiveExactMatch
            }
        }
    }

    /**
     * Creates a new node with the given values.
     *
     * Make sure to specify **all** `OptionKey` values for `subtreeOptions`, especially when the node is intended to be the root.
     * These subtree options will be used for all descendants unless otherwise specified. If any subtree option keys are missing,
     * a default value will be provided.
     */
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
        val copiedNode = NodeConfig(name = name, options = HashMap(options), subtreeOptions = HashMap(subtreeOptions), children = children.map { it.deepCopy() }.toMutableSet(), wildcardChildren = wildcardChildren?.deepCopy())
        return copiedNode
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
                if (pathConfig.scope == NodeConfig.Scope.subtree) {
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
                        node.options[NodeConfig.OptionKey.anyOrderMatch] =
                            NodeConfig.Config(isActive = true)
                    } else {
                        child.options[NodeConfig.OptionKey.anyOrderMatch] =
                            NodeConfig.Config(isActive = true)
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
    private fun getProcessedPathComponents(pathString: String?): List<PathComponent> {
        val objectPathComponents = getObjectPathComponents(pathString)
        val pathComponents = mutableListOf<PathComponent>()
        for (objectPathComponent in objectPathComponents) {
            val key = objectPathComponent.replace("\\.", ".")
            // Extract the string part and array component part(s) from the key string
            val components = getArrayPathComponents(key)
            // Process string segment
            components.stringComponent?.let { stringComponent ->
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
            for (arrayComponent in components.arrayComponents) {
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
                                name = it.index.toString(),
                                isAnyOrder = it.isAnyOrder,
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
        // TODO: shouldn't be possible for subtree options to not exist - check if config false is necessary here
        node.wildcardChildren?.subtreeOptions?.set(key, node.subtreeOptions[key] ?: Config(isActive = false))
        for (child in node.children) {
            // Only propagate the subtree value for the current option key,
            // otherwise, previously set subtree values will be reset to the default values
            child.subtreeOptions[key] = node.subtreeOptions[key] ?: Config(isActive = false)
            propagateSubtreeOption(child, pathConfig)
        }
    }

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


}