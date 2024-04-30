/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile.util

import com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch
import com.adobe.marketing.mobile.util.JSONAsserts.assertTypeMatch
import com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree
import com.adobe.marketing.mobile.util.NodeConfig.Scope.SingleNode
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * This test suite validates the common logic for all path options. It covers:
 * 1. Node scope comparison: SingleNode versus Subtree.
 * 2. Application of multiple options simultaneously.
 * 3. Option overriding.
 * 4. Specifying multiple paths for a single option.
 */
class JSONAssertsPathOptionsTests {
    /**
     * Validates that simple index array paths in comparison options work correctly.
     */
    @Test
    fun testSatisfiedPathOption_PassesWithArray() {
        val expected = "[1]"
        val actual = "[2]"

        assertExactMatch(expected, actual, ValueTypeMatch("[0]"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[0]"))
        }
    }

    /**
     * Validates that simple dictionary key paths in comparison options work correctly.
     */
    @Test
    fun testSatisfiedPathOption_PassesWithDictionary() {
        val expected = """
        {
          "key0": 1
        }
        """

        val actual = """
        {
          "key0": 2
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key0"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0"))
        }
    }

    /**
     * Validates that key paths specifying nested dictionary keys in the JSON hierarchy work correctly.
     */
    @Test
    fun testSatisfiedNestedPathOption_PassesWithDictionary() {
        val expected = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 1
          }
        }
        """

        val actual = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 2
          }
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key0-1.key1-0"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0-1.key1-0"))
        }
    }

    /**
     * Validates that key paths specifying nested array keys in the JSON hierarchy work correctly.
     */
    @Test
    fun testSatisfiedNestedPathOption_PassesWithArray() {
        val expected = "[1, [1]]"
        val actual = "[1, [2]]"

        assertExactMatch(expected, actual, ValueTypeMatch("[1][0]"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[1][0]"))
        }
    }

    /**
     * Validates that an unsatisfied path option with a specific path applied to arrays correctly
     * triggers a test failure.
     */
    @Test
    fun testUnsatisfiedPathOption_FailsWithArray() {
        val expected = "[1, [1]]"
        val actual = "[1, [1, 1]]"

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount("[1]"))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount("[1]"))
        }
    }

    /**
     * Validates that an unsatisfied path option with a default path applied to arrays correctly
     * triggers a test failure.
     */
    @Test
    fun testUnsatisfiedPathOption_UsingDefaultPathOption_FailsWithArray() {
        val expected = "[1]"
        val actual = "[1, 2]"

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount())
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount())
        }
    }

    /**
     * Validates that an unsatisfied path option with a specific path applied to dictionaries correctly
     * triggers a test failure.
     */
    @Test
    fun testUnsatisfiedPathOption_FailsWithDictionary() {
        val expected = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 1
          }
        }
        """
        val actual = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 1,
            "key1-1": 1
          }
        }
        """

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount("key0-1"))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount("key0-1"))
        }
    }

    /**
     * Validates that an unsatisfied path option with a default path applied to dictionaries correctly
     * triggers a test failure.
     */
    @Test
    fun testUnsatisfiedPathOption_UsingDefaultPathOption_FailsWithDictionary() {
        val expected = """
        {
          "key0-0": 1
        }
        """
        val actual = """
        {
          "key0-0": 1,
          "key0-1": 1
        }
        """

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount())
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount())
        }
    }

    /**
     * Validates that an unsatisfied path option with a nested array path applied to arrays correctly
     * triggers a test failure.
     */
    @Test
    fun testUnsatisfiedNestedPathOption_FailsWithArray() {
        val expected = "[1, [[1]]]"
        val actual = "[1, [[1, 2]]]"

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount("[1][0]"))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount("[1][0]"))
        }
    }

    /**
     * Validates that an unsatisfied path option with a nested dictionary key path applied to dictionaries
     * correctly triggers a test failure.
     */
    @Test
    fun testUnsatisfiedNestedPathOption_FailsWithDictionary() {
        val expected = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": {
              "key2-0": 1
            }
          }
        }
        """

        val actual = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": {
              "key2-0": 1,
              "key2-1": 1
            }
          }
        }
        """

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount("key0-1.key1-0"))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount("key0-1.key1-0"))
        }
    }

    /**
     * Validates that path options with nonexistent paths applied to arrays do not affect validation results.
     */
    @Test
    fun testNonexistentExpectedPathDoesNotAffectValidation_withArray() {
        val expected = "[1]"
        val actual = "[1, [1]]"

        assertExactMatch(expected, actual, CollectionEqualCount("[1]"))
        assertTypeMatch(expected, actual, CollectionEqualCount("[1]"))
    }

    /**
     * Validates that path options with nonexistent paths applied to dictionaries do not affect validation results.
     */
    @Test
    fun testNonexistentExpectedPathDoesNotAffectValidation_withDictionary() {
        val expected = """
        {
          "key0-0": 1
        }
        """

        val actual = """
        {
          "key0-0": 1,
          "key0-1": 1
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount("key-doesnt-exist"))
        assertTypeMatch(expected, actual, CollectionEqualCount("key-doesnt-exist"))
    }

    /**
     * Validates that path options with dictionary key paths applied to arrays do not affect validation results.
     */
    @Test
    fun testInvalidExpectedPathDoesNotAffectValidation_withArray() {
        val expected = "[1]"
        val actual = "[1, [1]]"

        assertExactMatch(expected, actual, CollectionEqualCount("key0"))
        assertTypeMatch(expected, actual, CollectionEqualCount("key0"))
    }

    /**
     * Validates that path options with array key paths applied to dictionaries do not affect validation results.
     */
    @Test
    fun testInvalidExpectedPathDoesNotAffectValidation_withDictionary() {
        val expected = """
        {
          "key0-0": 1
        }
        """

        val actual = """
        {
          "key0-0": 1,
          "key0-1": 1
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount("[0]"))
        assertTypeMatch(expected, actual, CollectionEqualCount("[0]"))
    }

    /**
     * Validates that default path options for the same option type are overridden by the latest path
     * option provided.
     */
    @Test
    fun testOrderDependentOptionOverride() {
        val expected = """
        {
          "key1": 1
        }
        """

        val actual = """
        {
          "key1": 1,
          "key2": 2
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount(), CollectionEqualCount(isActive = false))
        assertTypeMatch(expected, actual, CollectionEqualCount(), CollectionEqualCount(isActive = false))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount(isActive = false), CollectionEqualCount())
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount(isActive = false), CollectionEqualCount())
        }
    }

    /**
     * Validates that path options for the same path and option type are overridden by the latest path
     * option provided.
     */
    @Test
    fun testOrderDependentOptionOverride_WithSpecificKey() {
        val expected = """
        {
          "key1": [1]
        }
        """

        val actual = """
        {
          "key1": [1, 2]
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount("key1"), CollectionEqualCount("key1", isActive = false))
        assertTypeMatch(expected, actual, CollectionEqualCount("key1"), CollectionEqualCount("key1", isActive = false))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount("key1", isActive = false), CollectionEqualCount("key1"))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, CollectionEqualCount("key1", isActive = false), CollectionEqualCount("key1"))
        }
    }

    /**
     * Validates that variadic path options and array-style path options applied to arrays have the same results.
     */
    @Test
    fun testVariadicAndArrayPathOptionsBehaveTheSame_withArray() {
        val expected = "[1]"
        val actual = "[2]"

        assertExactMatch(expected, actual, listOf(ValueTypeMatch("[0]")))
        assertExactMatch(expected, actual, ValueTypeMatch("[0]"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, listOf(ValueExactMatch("[0]")))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[0]"))
        }
    }

    /**
     * Validates that variadic path options and array-style path options applied to dictionaries have the same results.
     */
    @Test
    fun testVariadicAndArrayPathOptionsBehaveTheSame_withDictionary() {
        val expected = """
        {
          "key0": 1
        }
        """

        val actual = """
        {
          "key0": 2
        }
        """

        assertExactMatch(expected, actual, listOf(ValueTypeMatch("key0")))
        assertExactMatch(expected, actual, ValueTypeMatch("key0"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, listOf(ValueExactMatch("key0")))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0"))
        }
    }

    /**
     * Validates that path options with variadic paths and array-style paths applied to arrays have the same results.
     */
    @Test
    fun testVariadicAndArrayPathsBehaveTheSame_withArray() {
        val expected = "[1, 1]"
        val actual = "[2, 2]"

        assertExactMatch(expected, actual, ValueTypeMatch(listOf("[0]", "[1]")))
        assertExactMatch(expected, actual, ValueTypeMatch("[0]", "[1]"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch(listOf("[0]", "[1]")))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[0]", "[1]"))
        }
    }

    /**
     * Validates that path options with variadic paths and array-style paths applied to dictionaries have the same results.
     */
    @Test
    fun testVariadicAndArrayPathsBehaveTheSame_withDictionary() {
        val expected = """
        {
          "key0": 1,
          "key1": 1
        }
        """

        val actual = """
        {
          "key0": 2,
          "key1": 2
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch(listOf("key0", "key1")))
        assertExactMatch(expected, actual, ValueTypeMatch("key0", "key1"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch(listOf("key0", "key1")))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0", "key1"))
        }
    }

    /**
     * Validates that a path option with subtree scope applied to arrays works correctly.
     */
    @Test
    fun testSubtreeOptionPropagates_WithArray() {
        val expected = "[1, [1]]"
        val actual = "[1, [2]]"

        assertExactMatch(expected, actual, ValueTypeMatch(scope = Subtree))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch(scope = Subtree))
        }
    }

    /**
     * Validates that a path option with subtree scope applied to dictionaries works correctly.
     */
    @Test
    fun testSubtreeOptionPropagates_WithDictionary() {
        val expected = """
        {
          "key0-0": {
            "key1-0": 1
          }
        }
        """

        val actual = """
        {
          "key0-0": {
            "key1-0": 2
          }
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch(scope = Subtree))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch(scope = Subtree))
        }
    }

    /**
     * Validates that a path option with single node scope applied to dictionaries works correctly.
     */
    @Test
    fun testSingleNodeOption_DoesNotPropagate_WithDictionary() {
        val expected = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 1
          }
        }
        """

        val actual = """
        {
          "key0-0": 1,
          "key0-1": {
            "key1-0": 2
          }
        }
        """

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, ValueTypeMatch(scope = SingleNode))
        }
        assertTypeMatch(expected, actual, ValueExactMatch(scope = SingleNode))
    }

    /**
     * Validates that a path option with single node scope applied to arrays works correctly.
     */
    @Test
    fun testSingleNodeOption_DoesNotPropagate_WithArray() {
        val expected = "[1, [1]]"
        val actual = "[1, [2]]"

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, ValueTypeMatch(scope = SingleNode))
        }
        assertTypeMatch(expected, actual, ValueExactMatch(scope = SingleNode))
    }

    /**
     * Validates that a subtree scope path option can be overridden at the single node level in dictionaries.
     */
    @Test
    fun testSubtreeOption_OverriddenBySingleNode() {
        val expected = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 1
            }
          }
        }
        """

        val actual = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 1
            },
            "key1-1": 1
          }
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree), CollectionEqualCount("key0-0", isActive = false))

        // Sanity check: Override without `SingleNode` should fail
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree))
        }
    }

    /**
     * Validates that a subtree scope path option can be overridden at the subtree level in dictionaries.
     */
    @Test
    fun testSubtreeOption_OverriddenAtDifferentLevels() {
        val expected = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 1
            }
          }
        }
        """

        val actual = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 1,
              "key2-1": 1
            },
            "key1-1": 1
          }
        }
        """

        assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree), CollectionEqualCount("key0-0", isActive = false, scope = Subtree))

        // Sanity check: Override without `Subtree` should fail
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree), CollectionEqualCount("key0-0", isActive = false))
        }
    }

    /**
     * Validates that different path options with overlapping subtree scopes in dictionaries are order-independent
     * and do not interfere with each other.
     */
    @Test
    fun testSubtreeValues_NotIncorrectlyOverridden_WhenSettingMultiple() {
        val expected = """
        {
          "key1": {
            "key2": {
              "key3": [
                {
                  "key4": "STRING_TYPE"
                }
              ]
            }
          }
        }
        """

        val actual = """
        {
          "key1": {
            "key2": {
              "key3": [
                {
                  "key4": "abc"
                }
              ]
            }
          }
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch(scope = Subtree), CollectionEqualCount(scope = Subtree))
        assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree), ValueTypeMatch(scope = Subtree))
    }

    /**
     * Validates that different path options with different but overlapping subtree scopes in dictionaries
     * are order-independent and do not interfere with each other.
     */
    @Test
    fun testSubtreeValues_whenDifferentLevels_NotIncorrectlyOverridden_WhenSettingMultiple() {
        val expected = """
        {
          "key1": {
            "key2": {
              "key3": [
                {
                  "key4": "STRING_TYPE"
                }
              ]
            }
          }
        }
        """

        val actual = """
        {
          "key1": {
            "key2": {
              "key3": [
                {
                  "key4": "abc"
                }
              ]
            }
          }
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key1.key2.key3", scope = Subtree), CollectionEqualCount(scope = Subtree))
        assertExactMatch(expected, actual, CollectionEqualCount(scope = Subtree), ValueTypeMatch("key1.key2.key3", scope = Subtree))
    }

    /**
     * Validates that path options are order-independent.
     */
    @Test
    fun testPathOptions_OrderIndependence() {
        val expected = """
        {
          "key0": 1,
          "key1": 1
        }
        """

        val actual = """
        {
          "key0": 2,
          "key1": 2
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key0"), ValueTypeMatch("key1"))
        assertExactMatch(expected, actual, ValueTypeMatch("key1"), ValueTypeMatch("key0"))
    }

    /**
     * Validates that different path options can apply to the same path.
     */
    @Test
    fun testPathOptions_OverlappingConditions() {
        val expected = """
        {
          "key1": [2]
        }
        """

        val actual = """
        {
          "key1": ["a", "b", 1]
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key1[0]"), AnyOrderMatch("key1[0]"))
    }

    /**
     * Validates that a path option can specify multiple paths at once when applied to arrays.
     */
    @Test
    fun testMultiPath_whenArray() {
        val expected = "[1, 1]"
        val actual = "[2, 2]"

        assertExactMatch(expected, actual, ValueTypeMatch("[0]", "[1]"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[0]", "[1]"))
        }
    }

    /**
     * Validates that a path option can specify multiple paths at once when applied to dictionaries.
     */
    @Test
    fun testMultiPath_whenDictionary() {
        val expected = """
        {
          "key0": 1,
          "key1": 1
        }
        """

        val actual = """
        {
          "key0": 2,
          "key1": 2
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key0", "key1"))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0", "key1"))
        }
    }

    /**
     * Validates that a path option specifying multiple paths in an array properly propagates the subtree scope to each path.
     */
    @Test
    fun testMultiPath_SubtreePropagates_whenArray() {
        val expected = """
        [
          [
            [1], [1]
          ],
          [
            [1], [1]
          ]
        ]
        """

        val actual = """
        [
          [
            [2], [2]
          ],
          [
            [2], [2]
          ]
        ]
        """

        assertExactMatch(expected, actual, ValueTypeMatch("[0]", "[1]", scope = Subtree))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("[0]", "[1]", scope = Subtree))
        }
    }

    /**
     * Validates that a path option specifying multiple paths in a dictionary properly propagates the subtree scope to each path.
     */
    @Test
    fun testMultiPath_SubtreePropagates_whenDictionary() {
        val expected = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 1
            },
            "key1-1": {
              "key2-0": 1
            }
          },
          "key0-1": {
            "key1-0": {
              "key2-0": 1
            },
            "key1-1": {
              "key2-0": 1
            }
          }
        }
        """

        val actual = """
        {
          "key0-0": {
            "key1-0": {
              "key2-0": 2
            },
            "key1-1": {
              "key2-0": 2
            }
          },
          "key0-1": {
            "key1-0": {
              "key2-0": 2
            },
            "key1-1": {
              "key2-0": 2
            }
          }
        }
        """

        assertExactMatch(expected, actual, ValueTypeMatch("key0-0", "key0-1", scope = Subtree))

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            assertTypeMatch(expected, actual, ValueExactMatch("key0-0", "key0-1", scope = Subtree))
        }
    }

    /**
     * Validates that path options set to inactive (`isActive = false`) are correctly applied in validation logic.
     */
    @Test
    fun testSetting_isActiveToFalse() {
        val expected = "[1]"
        val actual = "[1, [1]]"

        assertExactMatch(expected, actual, CollectionEqualCount(isActive = false))
        assertTypeMatch(expected, actual, CollectionEqualCount(isActive = false))
    }
}