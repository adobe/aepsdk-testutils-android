/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Assert.assertEquals

object JSONAsserts {
    /**
     * Asserts exact equality between two [JSONObject] or [JSONArray] instances.
     *
     * @param expected The expected [JSONObject] or [JSONArray] to compare.
     * @param actual The actual [JSONObject] or [JSONArray] to compare.
     *
     * @throws AssertionError If the [expected] and [actual] JSON structures are not exactly equal.
     */
    @JvmStatic
    fun assertEqual(expected: Any?, actual: Any?) {
        if (expected == null && actual == null) {
            return
        }
        if (expected == null || actual == null) {
            fail("""
                ${if (expected == null) "Expected is null" else "Actual is null"} and 
                ${if (expected == null) "Actual" else "Expected"} is non-null.
        
                Expected: ${expected.toString()}
        
                Actual: ${actual.toString()}
            """.trimIndent())
            return
        }
        // Exact equality is just a special case of exact match
        assertExactMatch(expected, actual, CollectionEqualCount(isActive = true, scope = NodeConfig.Scope.Subtree))
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required.
     * By default, the comparison logic uses the value type match option, only validating that both values are of the same type.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Alternate mode paths enable switching from the default type matching mode to exact matching
     * mode for specified paths onward.
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example `exactMatchPaths` path for this JSON would be: `"key2[1].nest2"`.
     *
     * Alternate mode paths must begin from the top level of the expected JSON. Multiple paths can be defined.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * For any position array element matching:
     * 1. Specific index: `[*<INT>]` (ex: `[*0]`, `[*28]`). Only a single `*` character MUST be placed to the
     * left of the index value. The element at the given index in `expected` will use any position matching in `actual`.
     * 2. All elements: `[*]`. All elements in `expected` will use any position matching in `actual`.
     *
     * When combining any position option indexes and standard indexes, standard indexes are validated first.
     *
     * @param expected The expected JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     * @param actual The actual JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     * @param exactMatchPaths The key paths in the expected JSON that should use exact matching mode, where values require both the same type and literal value.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("assertTypeMatchWithExactMatchPaths")
    fun assertTypeMatch(expected: Any, actual: Any?, exactMatchPaths: List<String> = emptyList()) {
        val treeDefaults = listOf(
            AnyOrderMatch(isActive = false),
            CollectionEqualCount(isActive = false),
            KeyMustBeAbsent(isActive = false),
            ValueTypeMatch()
        )
        validate(expected, actual, listOf(ValueExactMatch(exactMatchPaths)), treeDefaults, true)
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required by default.
     * By default, the comparison logic uses the value type match option, only validating that both values are of the same type.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Path options allow for powerful customizations to the comparison logic; see structs conforming to [MultiPathConfig]:
     * - [AnyOrderMatch]
     * - [CollectionEqualCount]
     * - [KeyMustBeAbsent]
     * - [ValueExactMatch], [ValueTypeMatch]
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example path for this JSON would be: `"key2[1].nest2"`.
     *
     * Paths must begin from the top level of the expected JSON. Multiple paths and path options can be used at the same time.
     * Path options are applied sequentially. If an option overrides an existing one, the overriding will occur in the order in which
     * the path options are specified.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * Formats for wildcard object key and array index names:
     * - Array wildcard - All children elements of the array: `[*]` (ex: `key1[*].key3`)
     * - Object wildcard - All children elements of the object: `*` (ex: `key1.*.key3`)
     * - Key whose name is asterisk - Escape the asterisk with backslash: `"\*"`
     * - Note that wildcard path options also apply to any existing specific nodes at the same level.
     *
     * - Parameters:
     *   - expected: The expected JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - actual: The actual JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - pathOptions: The path options to use in the validation process.
     */
    @JvmStatic
    fun assertTypeMatch(expected: Any, actual: Any?, pathOptions: List<MultiPathConfig>) {
        val treeDefaults = listOf(
            AnyOrderMatch(isActive = false),
            CollectionEqualCount(isActive = false),
            KeyMustBeAbsent(isActive = false),
            ValueTypeMatch()
        )
        validate(expected, actual, pathOptions.toList(), treeDefaults, false)
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required by default.
     * By default, the comparison logic uses the value type match option, only validating that both values are of the same type.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Path options allow for powerful customizations to the comparison logic; see structs conforming to [MultiPathConfig]:
     * - [AnyOrderMatch]
     * - [CollectionEqualCount]
     * - [KeyMustBeAbsent]
     * - [ValueExactMatch], [ValueTypeMatch]
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example path for this JSON would be: `"key2[1].nest2"`.
     *
     * Paths must begin from the top level of the expected JSON. Multiple paths and path options can be used at the same time.
     * Path options are applied sequentially. If an option overrides an existing one, the overriding will occur in the order in which
     * the path options are specified.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * Formats for wildcard object key and array index names:
     * - Array wildcard - All children elements of the array: `[*]` (ex: `key1[*].key3`)
     * - Object wildcard - All children elements of the object: `*` (ex: `key1.*.key3`)
     * - Key whose name is asterisk - Escape the asterisk with backslash: `"\*"`
     * - Note that wildcard path options also apply to any existing specific nodes at the same level.
     *
     * - Parameters:
     *   - expected: The expected JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - actual: The actual JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - pathOptions: The path options to use in the validation process.
     */
    @JvmStatic
    fun assertTypeMatch(expected: Any, actual: Any?, vararg pathOptions: MultiPathConfig) {
        assertTypeMatch(expected, actual, pathOptions.toList())
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required.
     * By default, the comparison logic uses value exact match mode, validating that both values are of the same type
     * **and** have the same literal value.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Alternate mode paths enable switching from the default exact matching mode to type matching
     * mode for specified paths onward.
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example `typeMatchPaths` path for this JSON would be: `"key2[1].nest2"`.
     *
     * Alternate mode paths must begin from the top level of the expected JSON. Multiple paths can be defined.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * For any position array element matching:
     * 1. Specific index: `[*<INT>]` (ex: `[*0]`, `[*28]`). Only a single `*` character MUST be placed to the
     * left of the index value. The element at the given index in `expected` will use any position matching in `actual`.
     * 2. All elements: `[*]`. All elements in `expected` will use any position matching in `actual`.
     *
     * When combining any position option indexes and standard indexes, standard indexes are validated first.
     *
     * @param expected The expected JSON in [JSONObject] or [JSONArray] format to compare.
     * @param actual The actual JSON in [JSONObject] or [JSONArray] format to compare.
     * @param typeMatchPaths The key paths in the expected JSON that should use type matching mode, where values require only the same type (and are non-null if the expected value is not null).
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("assertExactMatchWithTypeMatchPaths")
    fun assertExactMatch(expected: Any, actual: Any?, typeMatchPaths: List<String> = emptyList()) {
        val treeDefaults = listOf(
            AnyOrderMatch(isActive = false),
            CollectionEqualCount(isActive = false),
            KeyMustBeAbsent(isActive = false),
            ValueExactMatch()
        )
        validate(expected, actual, listOf(ValueTypeMatch(typeMatchPaths)), treeDefaults, true)
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required by default.
     * By default, the comparison logic uses the value exact match option, validating that both values are of the same type
     * **and** have the same literal value.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Path options allow for powerful customizations to the comparison logic; see structs conforming to [MultiPathConfig]:
     * - [AnyOrderMatch]
     * - [CollectionEqualCount]
     * - [KeyMustBeAbsent]
     * - [ValueExactMatch], [ValueTypeMatch]
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example path for this JSON would be: `"key2[1].nest2"`.
     *
     * Paths must begin from the top level of the expected JSON. Multiple paths and path options can be used at the same time.
     * Path options are applied sequentially. If an option overrides an existing one, the overriding will occur in the order in which
     * the path options are specified.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * Formats for wildcard object key and array index names:
     * - Array wildcard - All children elements of the array: `[*]` (ex: `key1[*].key3`)
     * - Object wildcard - All children elements of the object: `*` (ex: `key1.*.key3`)
     * - Key whose name is asterisk - Escape the asterisk with backslash: `"\*"`
     * - Note that wildcard path options also apply to any existing specific nodes at the same level.
     *
     * - Parameters:
     *   - expected: The expected JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - actual: The actual JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - pathOptions: The path options to use in the validation process.
     */
    @JvmStatic
    fun assertExactMatch(expected: Any, actual: Any?, pathOptions: List<MultiPathConfig>) {
        val treeDefaults = listOf(
            AnyOrderMatch(isActive = false),
            CollectionEqualCount(isActive = false),
            KeyMustBeAbsent(isActive = false),
            ValueExactMatch()
        )
        validate(expected, actual, pathOptions.toList(), treeDefaults, false)
    }

    /**
     * Performs JSON validation where only the values from the `expected` JSON are required by default.
     * By default, the comparison logic uses the value exact match option, validating that both values are of the same type
     * **and** have the same literal value.
     *
     * Both objects and arrays use extensible collections by default, meaning that only the elements in `expected` are
     * validated.
     *
     * Path options allow for powerful customizations to the comparison logic; see structs conforming to [MultiPathConfig]:
     * - [AnyOrderMatch]
     * - [CollectionEqualCount]
     * - [KeyMustBeAbsent]
     * - [ValueExactMatch], [ValueTypeMatch]
     *
     * For example, given an expected JSON like:
     * ```
     * {
     *   "key1": "value1",
     *   "key2": [{ "nest1": 1}, {"nest2": 2}],
     *   "key3": { "key4": 1 },
     *   "key.name": 1,
     *   "key[123]": 1
     * }
     * ```
     * An example path for this JSON would be: `"key2[1].nest2"`.
     *
     * Paths must begin from the top level of the expected JSON. Multiple paths and path options can be used at the same time.
     * Path options are applied sequentially. If an option overrides an existing one, the overriding will occur in the order in which
     * the path options are specified.
     *
     * Formats for object keys:
     * - Standard keys - The key name itself: `"key1"`
     * - Nested keys - Use dot notation: `"key3.key4"`.
     * - Keys with dots in the name: Escape the dot notation with a backslash: `"key\.name"`.
     *
     * Formats for arrays:
     * - Standard index - The index integer inside square brackets: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets in the name - Escape the brackets with backslashes: `key\[123\]`.
     *
     * Formats for wildcard object key and array index names:
     * - Array wildcard - All children elements of the array: `[*]` (ex: `key1[*].key3`)
     * - Object wildcard - All children elements of the object: `*` (ex: `key1.*.key3`)
     * - Key whose name is asterisk - Escape the asterisk with backslash: `"\*"`
     * - Note that wildcard path options also apply to any existing specific nodes at the same level.
     *
     * - Parameters:
     *   - expected: The expected JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - actual: The actual JSON ([JSONObject], [JSONArray], or types supported by [getJSONRepresentation]) to compare.
     *   - pathOptions: The path options to use in the validation process.
     */
    @JvmStatic
    fun assertExactMatch(expected: Any, actual: Any?, vararg pathOptions: MultiPathConfig) {
        assertExactMatch(expected, actual, pathOptions.toList())
    }

    private fun validate(
        expected: Any,
        actual: Any?,
        pathOptions: List<MultiPathConfig>,
        treeDefaults: List<MultiPathConfig>,
        isLegacyMode: Boolean
    ) {
        try {
            val nodeTree = generateNodeTree(pathOptions, treeDefaults, isLegacyMode)

            val expectedJSON = getJSONRepresentation(expected)
            if (expectedJSON == null) {
                fail("Failed to convert expected to valid JSON representation.")
            }
            val actualJSON = getJSONRepresentation(actual)

            validateActual(actualJSON, nodeTree = nodeTree)
            validateJSON(expectedJSON, actualJSON, nodeTree = nodeTree)
        } catch (e: java.lang.IllegalArgumentException) {
            fail("Invalid JSON provided: ${e.message}")
        }
    }

    /**
     * Performs a customizable validation between the given `expected` and `actual` values, using the configured options.
     * In case of a validation failure **and** if `shouldAssert` is `true`, a test failure occurs.
     *
     * @param expected The expected value to compare.
     * @param actual The actual value to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared. Defaults to an empty list.
     * @param nodeTree A tree of configuration objects used to control various validation settings.
     * @param shouldAssert Indicates if an assertion error should be thrown if `expected` and `actual` are not equal. Defaults to `true`.
     * @return `true` if `expected` and `actual` are equal based on the settings in `nodeTree`, otherwise returns `false`.
     */
    private fun validateJSON(
        expected: Any?,
        actual: Any?,
        keyPath: List<Any> = emptyList(),
        nodeTree: NodeConfig,
        shouldAssert: Boolean = true
    ): Boolean {
        if (expected == null || expected == JSONObject.NULL) {
            return true
        }
        if (actual == null || actual == JSONObject.NULL) {
            if (shouldAssert) {
                fail("""
                    Expected JSON is non-nil but Actual JSON is nil.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
            }
            return false
        }

        return when {
            expected is String && actual is String ||
                    expected is Boolean && actual is Boolean ||
                    expected is Int && actual is Int ||
                    expected is Double && actual is Double -> {
                if (nodeTree.primitiveExactMatch.isActive) {
                    if (shouldAssert) assertEquals(
                        "Key path: ${keyPathAsString(keyPath)}",
                        expected,
                        actual
                    )
                    expected == actual
                } else {
                    true
                }
            }
            expected is JSONObject && actual is JSONObject -> validateJSON(
                expected as? JSONObject,
                actual as? JSONObject,
                keyPath,
                nodeTree,
                shouldAssert
            )
            expected is JSONArray && actual is JSONArray -> validateJSON(
                expected as? JSONArray,
                actual as? JSONArray,
                keyPath,
                nodeTree,
                shouldAssert
            )
            else -> {
                if (shouldAssert) {
                    fail(
                        """
                    Expected and Actual types do not match.
                    Expected: $expected
                    Actual: ${actual}
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
                }
                false
            }
        }
    }

    /**
     * Performs a customizable validation between the given `expected` and `actual` arrays, using the configured options.
     * In case of a validation failure **and** if `shouldAssert` is `true`, a test failure occurs.
     *
     * @param expected The expected array to compare.
     * @param actual The actual array to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param nodeTree A tree of configuration objects used to control various validation settings.
     * @param shouldAssert Indicates if an assertion error should be thrown if `expected` and `actual` are not equal.
     * @return `true` if `expected` and `actual` are equal based on the settings in `nodeTree`, otherwise returns `false`.
     */
    private fun validateJSON(
        expected: JSONArray?,
        actual: JSONArray?,
        keyPath: List<Any>,
        nodeTree: NodeConfig,
        shouldAssert: Boolean = true
    ): Boolean {
        if (expected == null) {
            return true
        }
        if (actual == null) {
            if (shouldAssert) {
                fail("""
                Expected JSON is non-nil but Actual JSON is nil.
    
                Expected: $expected
    
                Actual: $actual
    
                Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
            }
            return false
        }

        if (nodeTree.collectionEqualCount.isActive) {
            if (expected.length() != actual.length()) {
                if (shouldAssert) {
                    fail("""
                    Expected JSON count does not match Actual JSON.

                    Expected count: ${expected.length()}
                    Actual count: ${actual.length()}

                    Expected: $expected

                    Actual: $actual

                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
                }
                return false
            }
        } else if (expected.length() > actual.length()) {
            if (shouldAssert) {
                fail("""
                Expected JSON has more elements than Actual JSON.

                Expected count: ${expected.length()}
                Actual count: ${actual.length()}

                Expected: $expected

                Actual: $actual

                Key path: ${keyPathAsString(keyPath)}
            """.trimIndent())
            }
            return false
        }

        val expectedIndexes = (0 until expected.length()).associate { index ->
            index.toString() to NodeConfig.resolveOption(
                NodeConfig.OptionKey.AnyOrderMatch,
                nodeTree.getChild(index),
                nodeTree
            )
        }.toMutableMap()
        val anyOrderIndexes = expectedIndexes.filter { it.value.isActive }

        for (key in anyOrderIndexes.keys) {
            expectedIndexes.remove(key)
        }

        val availableWildcardActualIndexes = mutableSetOf<String>().apply {
            addAll((0 until actual.length()).map { it.toString() })
            removeAll(expectedIndexes.keys)
        }

        var validationResult = true

        for ((index, config) in expectedIndexes) {
            val intIndex = index.toInt()
            validationResult = validateJSON(
                expected[intIndex],
                actual[intIndex],
                keyPath + intIndex,
                nodeTree.getNextNode(index),
                shouldAssert
            ) && validationResult
        }

        for ((index, config) in anyOrderIndexes) {
            val intIndex = index.toInt()

            val actualIndex = availableWildcardActualIndexes.firstOrNull {
                validateJSON(
                    expected[intIndex],
                    actual[it.toInt()],
                    keyPath + intIndex,
                    nodeTree.getNextNode(index),
                    shouldAssert = false
                )
            }
            if (actualIndex == null) {
                if (shouldAssert) {
                    fail("""
                    Wildcard ${if (NodeConfig.resolveOption(NodeConfig.OptionKey.PrimitiveExactMatch, nodeTree.getChild(index), nodeTree).isActive) "exact" else "type"}
                    match found no matches on Actual side satisfying the Expected requirement.
            
                    Requirement: $nodeTree
            
                    Expected: ${expected[intIndex]}
            
                    Actual (remaining unmatched elements): ${availableWildcardActualIndexes.map { actual[it.toInt()] }}
            
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
                }
                validationResult = false
                break
            } else {
                availableWildcardActualIndexes.remove(actualIndex)
            }

        }
        return validationResult
    }

    /**
     * Performs a customizable validation between the given `expected` and `actual` dictionaries, using the configured options.
     * In case of a validation failure **and** if `shouldAssert` is `true`, a test failure occurs.
     *
     * @param expected The expected dictionary to compare.
     * @param actual The actual dictionary to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param nodeTree A tree of configuration objects used to control various validation settings.
     * @param shouldAssert Indicates if an assertion error should be thrown if `expected` and `actual` are not equal.
     * @return `true` if `expected` and `actual` are equal based on the settings in `nodeTree`, otherwise returns `false`.
     */
    private fun validateJSON(
        expected: JSONObject?,
        actual: JSONObject?,
        keyPath: List<Any>,
        nodeTree: NodeConfig,
        shouldAssert: Boolean = true
    ): Boolean {
        if (expected == null) {
            return true
        }
        if (actual == null) {
            if (shouldAssert) {
                fail("""
                    Expected JSON is non-nil but Actual JSON is nil.
        
                    Expected: $expected
        
                    Actual: $actual
        
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
            }
            return false
        }

        if (nodeTree.collectionEqualCount.isActive) {
            if (expected.length() != actual.length()) {
                if (shouldAssert) {
                    fail("""
                    Expected JSON count does not match Actual JSON.

                    Expected count: ${expected.length()}
                    Actual count: ${actual.length()}

                    Expected: $expected

                    Actual: $actual

                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
                }
                return false
            }
        } else if (expected.length() > actual.length()) {
            if (shouldAssert) {
                fail("""
                Expected JSON has more elements than Actual JSON.

                Expected count: ${expected.length()}
                Actual count: ${actual.length()}

                Expected: $expected

                Actual: $actual

                Key path: ${keyPathAsString(keyPath)}
            """.trimIndent())
            }
            return false
        }

        var validationResult = true

        for (key in expected.keys()) {
            val value = expected.get(key)
            val actualValue = actual.opt(key)
            validationResult = validateJSON(
                value,
                actualValue,
                keyPath + key,
                nodeTree.getNextNode(key),
                shouldAssert
            ) && validationResult
        }
        return validationResult
    }


    /**
     * Validates the provided `actual` value against a specified `nodeTree` configuration.
     *
     * This method traverses a `NodeConfig` tree to validate the `actual` value according to the specified node configuration.
     * It handles different types of values including dictionaries and arrays, and applies the relevant validation rules
     * based on the configuration of each node in the tree.
     *
     * Note that this logic is meant to perform negative validation (for example, the absence of keys), and this means when `actual` nodes run out
     * validation automatically passes. Positive validation should use `expected` + `validateJSON`.
     *
     * @param actual The value to be validated, wrapped in `AnyCodable`.
     * @param keyPath An array representing the current traversal path in the node tree. Starts as an empty array.
     * @param nodeTree The root of the `NodeConfig` tree against which the validation is performed.
     * @return A `Boolean` indicating whether the `actual` value is valid based on the `nodeTree` configuration.
     */
    fun validateActual(
        actual: Any?,
        keyPath: List<Any> = listOf(),
        nodeTree: NodeConfig
    ): Boolean {
        val actualValue = actual ?: return true

        return when (actualValue) {
            // Handle dictionaries
            is JSONObject -> validateActual(
                actual = actualValue,
                keyPath = keyPath,
                nodeTree = nodeTree
            )
            // Handle arrays
            is JSONArray -> validateActual(
                actual = actualValue,
                keyPath = keyPath,
                nodeTree = nodeTree
            )
            else -> {
                // KeyMustBeAbsent check
                // Value type validations currently do not have any options that should be handled by `actual`
                // validation side - default is true
                true
            }
        }
    }

    /**
     * Validates a [JSONArray]'s values against the provided node configuration tree.
     *
     * This method iterates through each element in the given [JSONArray] and performs validation
     * based on the provided [NodeConfig].
     *
     * @param actual The [JSONArray] to be validated.
     * @param keyPath An array representing the current path in the node tree during the traversal.
     * @param nodeTree The current node in the [NodeConfig] tree against which the [actual] values are validated.
     * @return A [Boolean] indicating whether all elements in the [actual] array are valid according to the node tree configuration.
     */
    private fun validateActual(
        actual: JSONArray?,
        keyPath: List<Any>,
        nodeTree: NodeConfig
    ): Boolean {
        val actualValues = actual ?: return true

        var validationResult = true

        for (index in 0 until actualValues.length()) {
            validationResult = validateActual(
                actual = actualValues.get(index),
                keyPath = keyPath.plus(index),
                nodeTree = nodeTree.getNextNode(index)
            ) && validationResult
        }

        return validationResult
    }

    /**
     * Validates a dictionary of `AnyCodable` values against the provided node configuration tree.
     *
     * This method iterates through each key-value pair in the given dictionary and performs validation
     * based on the provided `NodeConfig`.
     *
     * @param actual The dictionary of `AnyCodable` values to be validated.
     * @param keyPath An array representing the current path in the node tree during the traversal.
     * @param nodeTree The current node in the `NodeConfig` tree against which the `actual` values are validated.
     * @return A `Boolean` indicating whether all key-value pairs in the `actual` dictionary are valid according to the node tree configuration.
     */
    private fun validateActual(
        actual: JSONObject?,
        keyPath: List<Any>,
        nodeTree: NodeConfig
    ): Boolean {
        val actualValues = actual ?: return true

        var validationResult = true

        for (key in actualValues.keys()) {
            // KeyMustBeAbsent check
            // Check for keys that must be absent in the current node
            val resolvedKeyMustBeAbsent = NodeConfig.resolveOption(NodeConfig.OptionKey.KeyMustBeAbsent, nodeTree.getChild(key), nodeTree)
            if (resolvedKeyMustBeAbsent.isActive) {
                fail("""
                Actual JSON should not have key with name: $key

                Actual: $actualValues

                Key path: ${keyPathAsString(keyPath + listOf(key))}
            """.trimIndent())
                validationResult = false
            }
            validationResult = validateActual(
                actual = actualValues.get(key),
                keyPath = keyPath.plus(key),
                nodeTree = nodeTree.getNextNode(key)
            ) && validationResult
        }
        return validationResult
    }

    /**
     * Generates a tree structure from an array of path `String`s.
     *
     * This function processes each path in `paths`, extracts its individual components using `processPathComponents`, and
     * constructs a nested dictionary structure. The constructed dictionary is then merged into the main tree. If the resulting tree
     * is empty after processing all paths, this function returns `null`.
     *
     * @param pathOptions An array of path `String`s to be processed. Each path represents a nested structure to be transformed
     * into a tree-like dictionary.
     * @param treeDefaults Defaults used for tree configuration.
     * @param isLegacyMode Flag to determine whether legacy mode is used.
     * @return A tree-like dictionary structure representing the nested structure of the provided paths. Returns `null` if the
     * resulting tree is empty.
     */
    private fun generateNodeTree(
        pathOptions: List<MultiPathConfig>,
        treeDefaults: List<MultiPathConfig>,
        isLegacyMode: Boolean
    ): NodeConfig {
        // Create the first node using the incoming defaults
        val subtreeOptions: MutableMap<NodeConfig.OptionKey, NodeConfig.Config> = mutableMapOf()
        for (treeDefault in treeDefaults) {
            val key = treeDefault.optionKey
            subtreeOptions[key] = treeDefault.config
        }
        val rootNode = NodeConfig(name = null, subtreeOptions = subtreeOptions)

        for (pathConfig in pathOptions) {
            rootNode.createOrUpdateNode(pathConfig, isLegacyMode)
        }

        return rootNode
    }

    /**
     * Converts a key path represented by a list of JSON object keys and array indexes into a human-readable string format.
     *
     * The key path is used to trace the recursive traversal of a nested JSON structure.
     * For instance, the key path for the value "Hello" in the JSON `{ "a": { "b": [ "World", "Hello" ] } }`
     * would be ["a", "b", 1].
     * This method would convert it to the string "a.b[1]".
     *
     * Special considerations:
     * 1. If a key in the JSON object contains a dot (.), it will be escaped with a backslash in the resulting string.
     * 2. Empty keys in the JSON object will be represented as "" in the resulting string.
     *
     * @param keyPath A list of keys or array indexes representing the path to a value in a nested JSON structure.
     *
     * @return A human-readable string representation of the key path.
     */
    private fun keyPathAsString(keyPath: List<Any>): String {
        var result = ""
        for (item in keyPath) {
            when (item) {
                is String -> {
                    if (result.isNotEmpty()) {
                        result += "."
                    }
                    result += when {
                        item.contains(".") -> item.replace(".", "\\.")
                        item.isEmpty() -> "\"\""
                        else -> item
                    }
                }
                is Int -> result += "[$item]"
            }
        }
        return result
    }

    private fun getJSONRepresentation(obj: Any?): Any? {
        return when (obj) {
            null -> null
            is JSONObject, is JSONArray -> obj
            is Map<*, *> -> {
                try {
                    // Validate all strings are keys before trying to convert to JSON
                    if (obj.keys.all { it is String }) {
                        JSONObject(obj as Map<String, Any?>)
                    } else {
                        throw IllegalArgumentException("Failed to convert to JSON representation: Invalid JSON dictionary keys. Keys must be strings. Found: ${obj.keys}")
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create JSONObject: $obj, with reason: ${e.message}")
                }
            }
            is List<*> -> try {
                JSONArray(obj)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to create JSONArray from List: $obj, with reason: ${e.message}")
            }
            is Array<*> -> try {
                JSONArray(obj.toList())
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to create JSONArray from Array: $obj, with reason: ${e.message}")
            }
            is String -> {
                try {
                    JSONObject(obj)  // Attempt to parse as JSONObject first.
                } catch (e: JSONException) {
                    try {
                        JSONArray(obj)  // Attempt to parse as JSONArray if JSONObject fails.
                    } catch (e: JSONException) {
                        throw IllegalArgumentException("Failed to convert to JSON representation: Invalid JSON string '$obj'")
                    }
                }
            }
            else -> IllegalArgumentException("Failed to convert to JSON representation: $obj, with reason: Unsupported type ${obj.javaClass.kotlin}")
        }
    }
    // endregion
}

