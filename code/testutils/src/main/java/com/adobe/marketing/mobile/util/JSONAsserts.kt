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
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Assert.assertEquals
import java.util.regex.PatternSyntaxException

object JSONAsserts {
    /**
     * Asserts exact equality between two [JSONObject] or [JSONArray] instances.
     *
     * In the event of an assertion failure, this function provides a trace of the key path,
     * which includes dictionary keys and array indexes, to aid debugging.
     *
     * @param expected The expected [JSONObject] or [JSONArray] to compare.
     * @param actual The actual [JSONObject] or [JSONArray] to compare.
     *
     * @throws AssertionError If the [expected] and [actual] JSON structures are not exactly equal.
     */
    @JvmStatic
    fun assertEqual(expected: Any?, actual: Any?) {
        assertEqual(expected = expected, actual = actual, keyPath = mutableListOf(), shouldAssert = true)
    }

    /**
     * Performs a flexible JSON comparison where only the key-value pairs from the expected JSON are required.
     * By default, the function validates that both values are of the same type.
     *
     * Alternate mode paths enable switching from the default type matching mode to exact value matching
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
     * An example [exactMatchPaths] path for this JSON would be: `"key2[1].nest2"`.
     *
     * Alternate mode paths must begin from the top level of the expected JSON.
     * Multiple paths can be defined. If two paths collide, the shorter one takes priority.
     *
     * Formats for keys:
     * - Nested keys: Use dot notation, e.g., "key3.key4".
     * - Keys with dots: Escape the dot, e.g., "key\.name".
     *
     * Formats for arrays:
     * - Index specification: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets: Escape the brackets, e.g., `key\[123\]`.
     *
     * For wildcard array matching, where position doesn't matter:
     * 1. Specific index with wildcard: `[*<INT>]` or `[<INT>*]` (ex: `[*1]`, `[28*]`). The element
     * at the given index in [expected] will use wildcard matching in [actual].
     * 2. Universal wildcard: `[*]`. All elements in [expected] will use wildcard matching in [actual].
     *
     * In array comparisons, elements are compared in order, up to the last element of the expected array.
     * When combining wildcard and standard indexes, regular indexes are validated first.
     *
     * @param expected The expected JSON in [JSONObject] or [JSONArray] format to compare.
     * @param actual The actual JSON in [JSONObject] or [JSONArray] format to compare.
     * @param exactMatchPaths The key paths in the expected JSON that should use exact matching mode, where values require both the same type and literal value.
     */
    @JvmStatic
    fun assertTypeMatch(expected: Any, actual: Any?, exactMatchPaths: List<String> = emptyList()) {
        val pathTree = generatePathTree(paths = exactMatchPaths)
        assertFlexibleEqual(expected = expected, actual = actual, pathTree = pathTree, exactMatchMode = false)
    }

    /**
     * Performs a flexible JSON comparison where only the key-value pairs from the expected JSON are required.
     * By default, the function uses exact match mode, validating that both values are of the same type
     * and have the same literal value.
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
     * An example [typeMatchPaths] path for this JSON would be: `"key2[1].nest2"`.
     *
     * Alternate mode paths must begin from the top level of the expected JSON.
     * Multiple paths can be defined. If two paths collide, the shorter one takes priority.
     *
     * Formats for keys:
     * - Nested keys: Use dot notation, e.g., "key3.key4".
     * - Keys with dots: Escape the dot, e.g., "key\.name".
     *
     * Formats for arrays:
     * - Index specification: `[<INT>]` (e.g., `[0]`, `[28]`).
     * - Keys with array brackets: Escape the brackets, e.g., `key\[123\]`.
     *
     * For wildcard array matching, where position doesn't matter:
     * 1. Specific index with wildcard: `[*<INT>]` or `[<INT>*]` (ex: `[*1]`, `[28*]`). The element
     * at the given index in [expected] will use wildcard matching in [actual].
     * 2. Universal wildcard: `[*]`. All elements in [expected] will use wildcard matching in [actual].
     *
     * In array comparisons, elements are compared in order, up to the last element of the expected array.
     * When combining wildcard and standard indexes, regular indexes are validated first.
     *
     * @param expected The expected JSON in [JSONObject] or [JSONArray] format to compare.
     * @param actual The actual JSON in [JSONObject] or [JSONArray] format to compare.
     * @param typeMatchPaths The key paths in the expected JSON that should use type matching mode, where values require only the same type (and are non-null if the expected value is not null).
     */
    @JvmStatic
    @JvmOverloads
    fun assertExactMatch(expected: Any, actual: Any?, typeMatchPaths: List<String> = emptyList()) {
        val pathTree = generatePathTree(paths = typeMatchPaths)
        assertFlexibleEqual(expected = expected, actual = actual, pathTree = pathTree, exactMatchMode = true)
    }

    /**
     * Compares the given [expected] and [actual] values for exact equality. If they are not equal and [shouldAssert] is `true`,
     * an assertion error is thrown.
     *
     * @param expected The expected value to compare.
     * @param actual The actual value to compare.
     * @param keyPath A list of keys indicating the path to the current value being compared. This is particularly
     * useful for nested JSON objects and arrays. Defaults to an empty list.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     *
     * @return Returns true if [expected] and [actual] are equal, otherwise returns false.
     *
     * @throws AssertionError If [shouldAssert] is true and the [expected] and [actual] values are not equal.
     */
    private fun assertEqual(expected: Any?, actual: Any?, keyPath: List<Any> = listOf(), shouldAssert: Boolean): Boolean {
        val expectedIsNull = expected == null || expected == JSONObject.NULL
        val actualIsNull = actual == null || actual == JSONObject.NULL
        if (expectedIsNull && actualIsNull) {
            return true
        }
        if (expectedIsNull || actualIsNull) {
            if (shouldAssert) {
                fail(
                    """
                    ${if (expectedIsNull) "Expected is null" else "Actual is null"} and 
                    ${if (expectedIsNull) "Actual" else "Expected"} is non-null.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent()
                )
            }
            return false
        }

        return when {
            expected is String && actual is String -> {
                if (shouldAssert) assertEquals("Key path: ${keyPathAsString(keyPath)}", expected, actual)
                expected == actual
            }
            expected is Boolean && actual is Boolean -> {
                if (shouldAssert) assertEquals("Key path: ${keyPathAsString(keyPath)}", expected, actual)
                expected == actual
            }
            expected is Int && actual is Int -> {
                if (shouldAssert) assertEquals("Key path: ${keyPathAsString(keyPath)}", expected, actual)
                expected == actual
            }
            expected is Double && actual is Double -> {
                if (shouldAssert) assertEquals("Key path: ${keyPathAsString(keyPath)}", expected, actual,
                    0.0
                )
                expected == actual
            }
            expected is JSONObject && actual is JSONObject -> assertEqual(expected, actual, keyPath, shouldAssert = shouldAssert)
            expected is JSONArray && actual is JSONArray -> assertEqual(expected, actual, keyPath, shouldAssert = shouldAssert)
            else -> {
                if (shouldAssert) {
                    fail(
                        """
                        Expected and Actual types do not match.
                        Expected: $expected
                        Actual: $actual
                        Key path: ${keyPathAsString(keyPath)}
                    """.trimIndent()
                    )
                }
                false
            }
        }
    }

    /**
     * Compares two [JSONObject] instances for exact equality. If they are not equal and [shouldAssert] is `true`,
     * an assertion error is thrown.
     *
     * @param expected The expected [JSONObject] to compare.
     * @param actual The actual [JSONObject] to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     *
     * @return Returns `true` if [expected] and [actual] are exactly equal, otherwise returns `false`.
     *
     * @throws AssertionError If [shouldAssert] is true and the [expected] and [actual] JSON objects are not equal.
     */
    private fun assertEqual(expected: JSONObject?, actual: JSONObject?, keyPath: List<Any>, shouldAssert: Boolean): Boolean {
        if (expected == null && actual == null) {
            return true
        }
        if (expected == null || actual == null) {
            if (shouldAssert) {
                fail(
                    """
                    ${if (expected == null) "Expected is null" else "Actual is null"} and 
                    ${if (expected == null) "Actual" else "Expected"} is non-null.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent()
                )
            }
            return false
        }
        if (expected.length() != actual.length()) {
            if (shouldAssert) {
                fail(
                    """
                    Expected and Actual counts do not match (exact equality).
                    Expected count: ${expected.length()}
                    Actual count: ${actual.length()}
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """.trimIndent()
                )
            }
            return false
        }
        var finalResult = true
        for (key in expected.keys()) {
            finalResult = assertEqual(
                expected = expected.get(key),
                actual = actual.opt(key),
                keyPath = keyPath.plus(key),
                shouldAssert = shouldAssert
            ) && finalResult
        }
        return finalResult
    }

    /**
     * Compares two [JSONArray] instances for exact equality. If they are not equal and [shouldAssert] is `true`,
     * an assertion error is thrown.
     *
     * @param expected The expected [JSONArray] to compare.
     * @param actual The actual [JSONArray] to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     *
     * @return Returns `true` if [expected] and [actual] are exactly equal, otherwise returns `false`.
     *
     * @throws AssertionError If [shouldAssert] is `true` and the [expected] and [actual] JSON arrays are not equal.
     */
    private fun assertEqual(
        expected: JSONArray?,
        actual: JSONArray?,
        keyPath: List<Any>,
        shouldAssert: Boolean
    ): Boolean {
        if (expected == null && actual == null) {
            return true
        }
        if (expected == null || actual == null) {
            if (shouldAssert) {
                fail("""
                ${if (expected == null) "Expected is null" else "Actual is null"} and ${if (expected == null) "Actual" else "Expected"} is non-null.
                Expected: ${expected.toString()}
                Actual: ${actual.toString()}
                Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
            }
            return false
        }
        if (expected.length() != actual.length()) {
            if (shouldAssert) {
                fail("""
                Expected and Actual counts do not match (exact equality).
                Expected count: ${expected.length()}
                Actual count: ${actual.length()}
                Expected: $expected
                Actual: $actual
                Key path: ${keyPathAsString(keyPath)}
                """.trimIndent())
            }
            return false
        }
        var finalResult = true
        for (index in 0 until expected.length()) {
            finalResult = assertEqual(
                expected = expected.get(index),
                actual = actual.get(index),
                keyPath = keyPath.plus(index),
                shouldAssert = shouldAssert
            ) && finalResult
        }
        return finalResult
    }

    // region Flexible assertion methods

    /**
     * Performs a flexible comparison between the given [expected] and [actual] values, optionally using exact match
     * or value type match modes. In case of a mismatch and if [shouldAssert] is `true`, an assertion error is thrown.
     *
     * It allows for customized matching behavior through the [pathTree] and [exactMatchMode] parameters.
     *
     * @param expected The expected value to compare.
     * @param actual The actual value to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared. Defaults to an empty list.
     * @param pathTree A map representing specific paths within the JSON structure that should be compared using the alternate mode.
     * @param exactMatchMode If `true`, performs an exact match comparison; otherwise, uses value type matching.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     * Defaults to true.
     *
     * @return Returns `true` if [expected] and [actual] are equal based on the matching mode and the [pathTree], otherwise returns `false`.
     *
     * @throws AssertionError If [shouldAssert] is `true` and the [expected] and [actual] values are not equal.
     */
    private fun assertFlexibleEqual(
        expected: Any?,
        actual: Any?,
        keyPath: List<Any> = listOf(),
        pathTree: Map<String, Any>?,
        exactMatchMode: Boolean,
        shouldAssert: Boolean = true): Boolean {
        val expectedIsNull = expected == null || expected == JSONObject.NULL
        val actualIsNull = actual == null || actual == JSONObject.NULL
        if (expectedIsNull) {
            return true
        }
        if (actualIsNull) {
            if (shouldAssert) {
                fail("""
                    Expected JSON is non-null but Actual JSON is null.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """)
            }
            return false
        }

        /**
         * Compares the [expected] and [actual] values for equality based on the [exactMatchMode].
         */
        fun compareValuesAssumingTypeMatch(): Boolean {
            if (exactMatchMode) {
                if (shouldAssert) {
                    assertEqual(expected, actual, keyPath, shouldAssert = shouldAssert)
                }
                return expected == actual
            }
            // The value type matching has already succeeded due to meeting the conditions in the switch case
            return true
        }

        when {
            expected is String && actual is String -> return compareValuesAssumingTypeMatch()
            expected is Boolean && actual is Boolean -> return compareValuesAssumingTypeMatch()
            expected is Int && actual is Int -> return compareValuesAssumingTypeMatch()
            expected is Double && actual is Double -> return compareValuesAssumingTypeMatch()
            expected is JSONArray && actual is JSONArray -> return assertFlexibleEqual(
                expected = expected,
                actual = actual,
                keyPath = keyPath,
                pathTree = pathTree,
                exactMatchMode = exactMatchMode,
                shouldAssert = shouldAssert)
            expected is JSONObject && actual is JSONObject -> return assertFlexibleEqual(
                expected = expected,
                actual = actual,
                keyPath = keyPath,
                pathTree = pathTree,
                exactMatchMode = exactMatchMode,
                shouldAssert = shouldAssert)
            else -> {
                if (shouldAssert) {
                    fail("""
                    Expected and Actual types do not match.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """)
                }
                return false
            }
        }
    }

    /**
     * Performs a flexible comparison between the given [expected] and [actual] [JSONArray]s, optionally using exact match
     * or value type match modes. In case of a mismatch and if [shouldAssert] is `true`, an assertion error is thrown.
     *
     * It allows for customized matching behavior through the [pathTree] and [exactMatchMode] parameters.
     *
     * @param expected The expected [JSONArray] to compare.
     * @param actual The actual [JSONArray] to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param pathTree A map representing specific paths within the JSON structure that should be compared using the alternate mode.
     * @param exactMatchMode If `true`, performs an exact match comparison; otherwise, uses value type matching.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     *
     * @return Returns `true` if [expected] and [actual] are equal based on the matching mode and the [pathTree], otherwise returns `false`.
     *
     * @throws AssertionError If [shouldAssert] is `true` and the [expected] and [actual] JSON arrays are not equal.
     */
    private fun assertFlexibleEqual(
        expected: JSONArray?,
        actual: JSONArray?,
        keyPath: List<Any>,
        pathTree: Map<String, Any>?,
        exactMatchMode: Boolean,
        shouldAssert: Boolean
    ): Boolean {
        if (expected == null) {
            return true
        }

        if (actual == null) {
            if (shouldAssert) {
                fail("""
                Expected JSON is non-null but Actual JSON is null.
                Expected: $expected
                Actual: $actual
                Key path: ${keyPathAsString(keyPath)}
            """.trimIndent())
            }
            return false
        }
        if (expected.length() > actual.length()) {
            if (shouldAssert) {
                fail("""
                Expected JSON has more elements than Actual JSON. Impossible for Actual to fulfill Expected requirements.
                Expected count: ${expected.length()}
                Actual count: ${actual.length()}
                Expected: $expected
                Actual: $actual
                Key path: ${keyPathAsString(keyPath)}
            """.trimIndent())
            }
            return false
        }
        // Convert the `actual` array into a mutable map, where the key is the array index and the
        // value is the corresponding element. Used to prevent double matching.
        val actualMap = (0 until actual.length()).associateBy({ it }, { actual[it] }).toMutableMap()

        var expectedIndexes = (0 until expected.length()).toSet()
        val wildcardIndexes: Set<Int>

        // Collect all the keys from `pathTree` that either:
        // 1. Mark the path end (where the value is a `String`), or
        // 2. Contain the asterisk (*) character.
        val pathEndKeys = pathTree?.filter{ (key, value) ->
            value is String || key.contains('*')
        }?.keys ?: setOf()

        // If general wildcard is present, it supersedes other paths
        if (pathEndKeys.contains("[*]")) {
            wildcardIndexes = (0 until expected.length()).toSet()
            expectedIndexes = setOf()
        }
        else {
            // TODO: update this to be flat? since there's only 1 operation now instead of 3
            // Strongly validates index notation: "[123]"
            val arrayIndexValueRegex = """^\[(.*?)\]$"""
            val indexValues = pathEndKeys
                .flatMap { key -> getCapturedRegexGroups(text = key, regexPattern = arrayIndexValueRegex) }
                .toSet()

            fun filterConvertAndIntersect(
                condition: (String) -> Boolean,
                replacement: (String) -> String = { it }
            ): Set<Int> {
                var result = indexValues.filter(condition).mapNotNull { replacement(it).toIntOrNull() }.toSet()
                val intersection = expectedIndexes.intersect(result)
                result = intersection
                expectedIndexes = expectedIndexes - intersection
                return result
            }

            wildcardIndexes = filterConvertAndIntersect({ it.contains('*') }, { it.replace("*", "") })
        }

        var finalResult = true
        // Expected side indexes that do not have alternate paths specified are matched first
        // to their corresponding actual side index
        for (index in expectedIndexes) {
            val isPathEnd = pathTree?.get("[$index]") is String
            finalResult = assertFlexibleEqual(
                expected = expected.opt(index),
                actual = actual.opt(index),
                keyPath = keyPath.plus(index),
                pathTree = pathTree?.get("[$index]") as? Map<String, Any>,
                exactMatchMode = isPathEnd != exactMatchMode,
                shouldAssert = shouldAssert) && finalResult
            actualMap.remove(index)
        }

        // Wildcard indexes are allowed to match the remaining actual side elements
        for (index in wildcardIndexes) {
            val pathTreeValue = pathTree?.get("[*]")
                ?: pathTree?.get("[*$index]")
                ?: pathTree?.get("[$index*]")

            val isPathEnd = pathTreeValue is String

            val result = actualMap.toList().indexOfFirst {
                assertFlexibleEqual(
                    expected = expected.opt(index),
                    actual = it.second,
                    keyPath = keyPath.plus(index),
                    pathTree = pathTreeValue as? Map<String, Any>,
                    exactMatchMode = isPathEnd != exactMatchMode,
                    shouldAssert = false)
            }
            if (result == -1) {
                if (shouldAssert) {
                    fail("""
                            Wildcard ${if (isPathEnd != exactMatchMode) "exact" else "type"} match found no matches on Actual side satisfying the Expected requirement.
                            Requirement: $pathTreeValue
                            Expected: ${expected.opt(index)}
                            Actual (remaining unmatched elements): ${actualMap.values}
                            Key path: ${keyPathAsString(keyPath)}
                        """.trimIndent())
                }
                finalResult = false
                break
            }
            actualMap.remove(result)
        }

        return finalResult
    }

    /**
     * Performs a flexible comparison between the given [expected] and [actual] [JSONObject]s, optionally using exact match
     * or value type match modes. In case of a mismatch and if [shouldAssert] is `true`, an assertion error is thrown.
     *
     * It allows for customized matching behavior through the [pathTree] and [exactMatchMode] parameters.
     *
     * @param expected The expected [JSONObject] to compare.
     * @param actual The actual [JSONObject] to compare.
     * @param keyPath A list of keys or array indexes representing the path to the current value being compared.
     * @param pathTree A map representing specific paths within the JSON structure that should be compared using the alternate mode.
     * @param exactMatchMode If `true`, performs an exact match comparison; otherwise, uses value type matching.
     * @param shouldAssert Indicates if an assertion error should be thrown if [expected] and [actual] are not equal.
     *
     * @return Returns `true` if [expected] and [actual] are equal based on the matching mode and the [pathTree], otherwise returns `false`.
     *
     * @throws AssertionError If [shouldAssert] is `true` and the [expected] and [actual] JSON objects are not equal.
     */
    private fun assertFlexibleEqual(
        expected: JSONObject?,
        actual: JSONObject?,
        keyPath: List<Any>,
        pathTree: Map<String, Any>?,
        exactMatchMode: Boolean,
        shouldAssert: Boolean): Boolean {
        if (expected == null) {
            return true
        }
        if (actual == null) {
            if (shouldAssert) {
                fail("""
                    Expected JSON is non-null but Actual JSON is null.
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """)
            }
            return false
        }
        if (expected.length() > actual.length()) {
            if (shouldAssert) {
                fail("""
                    Expected JSON has more elements than Actual JSON.
                    Expected count: ${expected.length()}
                    Actual count: ${actual.length()}
                    Expected: $expected
                    Actual: $actual
                    Key path: ${keyPathAsString(keyPath)}
                """)
            }
            return false
        }
        var finalResult = true
        val iterator: Iterator<String> = expected.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()

            val pathTreeValue = pathTree?.get(key)

            val isPathEnd = pathTreeValue is String

            finalResult = assertFlexibleEqual(
                expected = expected.opt(key),
                actual = actual.opt(key),
                keyPath = keyPath.plus(key),
                pathTree = pathTreeValue as? Map<String, Any>,
                exactMatchMode = isPathEnd != exactMatchMode,
                shouldAssert = shouldAssert) && finalResult
        }
        return finalResult
    }
    // endregion

    // region Private helpers

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
    // endregion
}

