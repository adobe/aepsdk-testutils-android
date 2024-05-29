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

import org.junit.Test
import kotlin.test.assertFailsWith

class PathOptionElementCountTests {
    @Test
    fun testElementCount_withArray_passes() {
        val actual = "[1, \"abc\", true, null]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(4))
    }

    @Test
    fun testElementCount_withNestedArray_passes() {
        val actual = "[1, [\"abc\", true, null]]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(4, NodeConfig.Scope.Subtree))
    }

    @Test
    fun testElementCount_withNestedArray_whenSingleNodeScope_passes() {
        val actual = "[1, [\"abc\", true, null]]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(1))
    }

    @Test
    fun testElementCount_withNestedArray_whenSingleNodeScope_innerPath_passes() {
        val actual = "[1, [\"abc\", true, null]]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(3, "[1]"))
    }

    @Test
    fun testElementCount_withNestedArray_whenSimultaneousSingleNodeScope_passes() {
        val actual = "[1, [\"abc\", true, null]]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(1), ElementCount(3, "[1]"))
    }

    @Test
    fun testElementCount_withNestedArray_whenSimultaneousSingleNodeAndSubtreeScope_passes() {
        val actual = "[1, [\"abc\", true, null]]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(1),
            ElementCount(4, NodeConfig.Scope.Subtree)
        )
    }

    @Test
    fun testElementCount_withArray_whenCountNotEqual_fails() {
        val actual = "[1, \"abc\", true, null]"

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            JSONAsserts.assertExactMatch("[]", actual, ElementCount(5))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            JSONAsserts.assertExactMatch("[]", actual, ElementCount(3))
        }
    }

    @Test
    fun testElementCount_withArray_whenSingleNodeDisabled_passes() {
        val actual = "[1, \"abc\", true, null]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(3, NodeConfig.Scope.Subtree),
            ElementCount(null, false, "[0]")
        )
    }

    @Test
    fun testElementCount_withNestedArray_whenMiddleCollectionDisablesElementCount_passes() {
        val actual = "[1, [\"abc\", [true, null]]]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(3, NodeConfig.Scope.Subtree),
            ElementCount(null, false, "[1]")
        )
    }

    @Test
    fun testElementCount_withNestedArray_whenNestedSandwichedSubtreeOverrides_passes() {
        val actual = "[1, [\"abc\", [true, null]]]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(1, NodeConfig.Scope.Subtree, "[1]"),
            ElementCount(null, false, NodeConfig.Scope.Subtree, "[1][1]")
        )
    }

    @Test
    fun testElementCount_withNestedArray_whenNestedSingleNodeOverrides_passes() {
        val actual = "[1, [\"abc\", [true, null]]]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(1, "[1]")
        )
    }

    @Test
    fun testElementCount_withNestedArray_whenNestedSubtreeOverrides_passes() {
        val actual = "[1, [\"abc\", [true, null]]]"

        JSONAsserts.assertExactMatch(
            "[]",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(3, NodeConfig.Scope.Subtree, "[1]")
        )
    }

    /**
     * Counts are checked only at the collection level, so any ElementCount conditions placed on elements
     * directly are ignored.
     */
    @Test
    fun testElementCount_withArray_whenAppliedToElement_passes() {
        val actual = "[1]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(100, "[0]"))
    }

    @Test
    fun testElementCount_withArray_whenWildcard_passes() {
        val actual = "[[1],[1]]"

        JSONAsserts.assertExactMatch("[]", actual, ElementCount(1, "[*]"))
    }

    @Test
    fun testElementCount_withDictionary_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": "abc",
            "key3": true,
            "key4": null
        }
        """

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(4))
    }

    @Test
    fun testElementCount_withNestedDictionary_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": true,
                "key2_3": null
            }
        }
        """

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(4, NodeConfig.Scope.Subtree))
    }

    @Test
    fun testElementCount_withNestedDictionary_whenSingleNodeScope_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": true,
                "key2_3": null
            }
        }
        """

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(1))
    }

    @Test
    fun testElementCount_withNestedDictionary_whenSingleNodeScope_innerPath_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": true,
                "key2_3": null
            }
        }
        """

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(3, "key2"))
    }

    @Test
    fun testElementCount_withNestedDictionary_whenSimultaneousSingleNodeScope_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": true,
                "key2_3": null
            }
        }
        """

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(1), ElementCount(3, "key2"))
    }

    @Test
    fun testElementCount_withNestedDictionary_whenSimultaneousSingleNodeAndSubtreeScope_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": true,
                "key2_3": null
            }
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(1),
            ElementCount(4, NodeConfig.Scope.Subtree)
        )
    }

    @Test
    fun testElementCount_withDictionary_whenCountNotEqual_fails() {
        val actual = """
        {
            "key1": 1,
            "key2": "abc",
            "key3": true,
            "key4": null
        }
        """

        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            JSONAsserts.assertExactMatch("{}", actual, ElementCount(5))
        }
        assertFailsWith<AssertionError>("Validation should fail when path option is not satisfied") {
            JSONAsserts.assertExactMatch("{}", actual, ElementCount(3))
        }
    }

    @Test
    fun testElementCount_withDictionary_whenSingleNodeDisabled_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": "abc",
            "key3": true,
            "key4": null
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(3, NodeConfig.Scope.Subtree),
            ElementCount(null, false, "key1")
        )
    }

    @Test
    fun testElementCount_withNestedDictionary_whenMiddleCollectionDisablesElementCount_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": {
                    "key3_1": true,
                    "key3_2": null
                }
            }
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(3, NodeConfig.Scope.Subtree),
            ElementCount(null, false, "key2")
        )
    }

    @Test
    fun testElementCount_withNestedDictionary_whenNestedSandwichedSubtreeOverrides_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": {
                    "key3_1": true,
                    "key3_2": null
                }
            }
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(1, NodeConfig.Scope.Subtree, "key2"),
            ElementCount(null, false, NodeConfig.Scope.Subtree, "key2.key2_2")
        )
    }

    @Test
    fun testElementCount_withNestedDictionary_whenNestedSingleNodeOverrides_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": {
                    "key3_1": true,
                    "key3_2": null
                }
            }
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(1, "key2"),
        )
    }

    @Test
    fun testElementCount_withNestedDictionary_whenNestedSubtreeOverrides_passes() {
        val actual = """
        {
            "key1": 1,
            "key2": {
                "key2_1": "abc",
                "key2_2": {
                    "key3_1": true,
                    "key3_2": null
                }
            }
        }
        """

        JSONAsserts.assertExactMatch(
            "{}",
            actual,
            ElementCount(null, false, NodeConfig.Scope.Subtree),
            ElementCount(3, NodeConfig.Scope.Subtree, "key2"),
        )
    }

    /**
     * Counts are checked only at the collection level, so any ElementCount conditions placed on elements
     * directly are ignored.
     */
    @Test
    fun testElementCount_withDictionary_whenAppliedToElement_passes() {
        val actual = """{ "key1": 1 }"""

        JSONAsserts.assertExactMatch("{}", actual, ElementCount(100, "key1"))
    }
}
