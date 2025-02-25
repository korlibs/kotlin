/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import junit.framework.TestCase.assertNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.KotlinAndroidVariantHierarchyDslImpl
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KotlinAndroidTargetHierarchyDsl {

    @Test
    fun `test -  module - not set`() = buildProjectWithMPP().runLifecycleAwareTest {
        val dsl = KotlinAndroidVariantHierarchyDslImpl(project.objects)
        project.kotlinPluginLifecycle.launch {
            assertNull(dsl.sourceSetTree.orNull)
            assertNull(dsl.sourceSetTree.awaitFinalValue())
        }
    }

    @Test
    fun `test - module - can be set in users afterEvaluate`() = buildProjectWithMPP().runLifecycleAwareTest {
        val dsl = KotlinAndroidVariantHierarchyDslImpl(project.objects)
        afterEvaluate { dsl.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree("x")) }
        dsl.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree("-set-before-after-evaluate-"))
        assertEquals("x", dsl.sourceSetTree.awaitFinalValue()?.name)
        assertEquals(KotlinPluginLifecycle.Stage.FinaliseDsl, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - module - is respected in default refines edges`() {
        val project = buildProject {
            setMultiplatformAndroidSourceSetLayoutVersion(2)
            applyMultiplatformPlugin()
            androidLibrary { compileSdk = 33 }
        }

        val kotlin = project.multiplatformExtension
        project.runLifecycleAwareTest {
            kotlin.androidTarget()

            kotlin.targetHierarchy.android {
                unitTest.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree.test)
                instrumentedTest.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree.test)
            }

            AfterFinaliseRefinesEdges.await()

            assertEquals(setOf("commonTest"), kotlin.sourceSets.getByName("androidInstrumentedTest").dependsOn.map { it.name }.toSet())
            assertEquals(setOf("commonTest"), kotlin.sourceSets.getByName("androidUnitTest").dependsOn.map { it.name }.toSet())
        }
    }

    @Test
    fun `test - module - is respected in targetHierarchy`() {
        val project = buildProject {
            setMultiplatformAndroidSourceSetLayoutVersion(2)
            applyMultiplatformPlugin()
            androidLibrary { compileSdk = 33 }
        }

        val kotlin = project.multiplatformExtension
        project.runLifecycleAwareTest {
            kotlin.androidTarget()

            kotlin.targetHierarchy.android {
                unitTest.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree("xxx"))
                instrumentedTest.sourceSetTree.set(KotlinTargetHierarchy.SourceSetTree("yyy"))
            }

            kotlin.targetHierarchy.default {
                withSourceSetTree(KotlinTargetHierarchy.SourceSetTree("xxx"))
                withSourceSetTree(KotlinTargetHierarchy.SourceSetTree("yyy"))
            }

            AfterFinaliseRefinesEdges.await()

            assertEquals(setOf("commonXxx"), kotlin.sourceSets.getByName("androidUnitTest").dependsOn.map { it.name }.toSet())
            assertEquals(setOf("commonYyy"), kotlin.sourceSets.getByName("androidInstrumentedTest").dependsOn.map { it.name }.toSet())
        }
    }
}