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

import com.adobe.marketing.mobile.util.JSONAsserts.assertEqual
import com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch
import com.adobe.marketing.mobile.util.JSONAsserts.assertTypeMatch
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
class JSONAssertsDeprecatedAPITests {
    // Value matching validation
    /**
     * Validates `null` equated to itself is true
     */
    @Test
    fun testAssertEqual_whenBothValuesAreNull_shouldPass() {
        val expected = null
        val actual = null

        assertEqual(expected, actual)
    }

    // Alternate path tests - assertEqual does not handle alternate paths and is not tested here

    /**
     * Validates alternate path wildcards function independently of order.
     *
     * Consequence: Tests can rely on unique sets of wildcard index values without the need to test
     * every variation.
     */
    @Test
    fun testAsserts_whenUsingAlternatePathWildcards_shouldFunctionIndependentlyOfOrder() {
        val expectedJSONString = """
        [1, 2]
        """.trimIndent()

        val actualJSONString = """
        ["a", "b", 1, 2]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*0]", "[*1]"))
        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*1]", "[*0]"))

        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*0]", "[*1]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*1]", "[*0]"))
    }

    /**
     * Validates specific index wildcards
     */
    @Test
    fun testAsserts_whenUsingSpecificIndexWildcards_shouldMatchDesignatedIndicesOnly() {
        val expectedJSONString = """
        [1, 2]
        """.trimIndent()

        val actualJSONString = """
        ["a", "b", 1, 2]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*0]", "[*1]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*0]", "[*1]"))
    }

    /**
     * Validates the general wildcard acts as a superset of any specific index wildcard.
     *
     * Consequence: Tests that require wildcard matching for all expected indexes
     * can use the general wildcard alone.
     */
    @Test
    fun testAsserts_whenUsingGeneralWildcard_shouldMatchAllDesignatedIndices() {
        val expectedJSONString = """
        [1, 2]
        """.trimIndent()

        val actualJSONString = """
        ["a", "b", 1, 2]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*]"))
    }

    /**
     * Validates that the wildcard character `*` can only be placed to the left of the index value.
     */
    @Test
    fun testAsserts_whenWildcardBeforeIndex_shouldMatchDesignatedIndex() {
        val expectedJSONString = """
        [1]
        """.trimIndent()

        val actualJSONString = """
        ["a", 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*0]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*0]"))
    }

    /**
     * Validates that incorrect placement of wildcard character `*` causes a test failure
     */
    @Test
    fun testAsserts_whenWildcardPlacementAfterIndex_shouldThrowAssertion() {
        val expectedJSONString = """
        [1]
        """.trimIndent()

        val actualJSONString = """
        ["a", 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertThrows(AssertionError::class.java) {
            assertExactMatch(expected, actual, typeMatchPaths = listOf("[0*]"))
        }
        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0*]"))
        }
    }

    // Array tests
    /**
     * Validates: specific index alternate path checks only against its paired index, as expected.
     */
    @Test
    fun testAsserts_whenSpecificIndexMismatches_shouldThrowAssertion() {
        val expectedJSONString = """
        [1]
        """.trimIndent()

        val actualJSONString = """
        ["a", 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertThrows(AssertionError::class.java) {
            assertExactMatch(expected, actual, typeMatchPaths = listOf("[0]"))
        }
        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0]"))
        }
    }

    /**
     * Validates: wildcard index allows for matching other positions.
     */
    @Test
    fun testAsserts_whenWildcardIndexUsed_shouldMatchAtAnyPosition() {
        val expectedJSONString = """
        [1]
        """.trimIndent()

        val actualJSONString = """
        ["a", 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*]"))
    }

    /**
     * Validates standard index matches take precedence over wildcard matches.
     *
     * Specifically, this checks the value at `actual[1]` is not first matched to the wildcard and
     * fails to satisfy the unspecified index `expected[1]`.
     */
    @Test
    fun testAsserts_whenStandardIndexMatchesOverWildcard_shouldPass() {
        val expectedJSONString = """
        [1, 1]
        """.trimIndent()

        val actualJSONString = """
        ["a", 1, 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*0]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*0]"))
    }

    /**
     * Validates:
     * 1. Specific index alternate paths should correctly match their corresponding indexes.
     * 2. Wildcard matching should correctly match with any appropriate index.
     */
    @Test
    fun testAsserts_whenSpecificIndexesMatchAndWildcardsAlignAppropriately_shouldPass() {
        val expectedJSONString = """
        [1, 2]
        """.trimIndent()

        val actualJSONString = """
        [4, 3, 2, 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[0]", "[1]"))
        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*]"))

        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0]", "[1]"))
        }
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*]"))
    }

    /**
     * Validates that specific index wildcards only apply to the index specified.
     */
    @Test
    fun testAsserts_whenIndexWildcardSpecified_shouldMatchDesignatedIndexOnly() {
        val expectedJSONString = """
        [1, 2]
        """.trimIndent()

        val actualJSONString = """
        [1, 3, 2, 1]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*1]"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*1]"))
    }

    /**
     * Validates that array-style access chained with key-value style access functions correctly.
     * This covers both specific index and wildcard index styles.
     */
    @Test
    fun testAsserts_whenChainingAccessWithSpecificAndWildcardIndices_shouldMatchDesignatedKeyOnly() {
        val expectedJSONString = """
        [
            {
                "key1": 1,
                "key2": 2,
                "key3": 3
            }
        ]
        """.trimIndent()

        val actualJSONString = """
        [
            {
                "key1": 1,
                "key2": 2,
                "key3": 3
            }
        ]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[0].key1"))
        assertExactMatch(expected, actual, typeMatchPaths = listOf("[*].key1"))

        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0].key1"))
        assertTypeMatch(expected, actual, exactMatchPaths = listOf("[*].key1"))
    }

    /**
     * Validates that chained array-style access functions correctly.
     */
    @Test
    fun testAsserts_whenChainingArrayAccessTwice_shouldMatchExactButThrowTypeMismatch() {
        val expectedJSONString = """
        [
            [1]
        ]
        """.trimIndent()

        val actualJSONString = """
        [
            [2]
        ]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[0][0]"))

        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0][0]"))
        }
    }

    /**
     * Validates that longer chained array-style access functions correctly.
     */
    @Test
    fun testAsserts_whenChainingArrayAccessFourTimes_shouldMatchExactButThrowTypeMismatch() {
        val expectedJSONString = """
        [[[[1]]]]
        """.trimIndent()

        val actualJSONString = """
        [[[[2]]]]
        """.trimIndent()
        val expected = JSONArray(expectedJSONString)
        val actual = JSONArray(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("[0][0][0][0]"))

        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("[0][0][0][0]"))
        }
    }

    /**
     * Validates that key-value style access chained with array-style access functions correctly.
     * This covers both specific index and wildcard index styles.
     */
    @Test
    fun testAsserts_whenChainingKeyValueWithArrayAccess_shouldMatchExactButThrowTypeMismatch() {
        val expectedJSONString = """
        {
            "key1": [1]
        }
        """.trimIndent()

        val actualJSONString = """
        {
            "key1": [2]
        }
        """.trimIndent()
        val expected = JSONObject(expectedJSONString)
        val actual = JSONObject(actualJSONString)

        assertExactMatch(expected, actual, typeMatchPaths = listOf("key1[0]"))
        assertExactMatch(expected, actual, typeMatchPaths = listOf("key1[*]"))

        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("key1[0]"))
        }
        assertThrows(AssertionError::class.java) {
            assertTypeMatch(expected, actual, exactMatchPaths = listOf("key1[*]"))
        }
    }
}
