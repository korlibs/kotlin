/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidTargetHierarchyDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidVariantHierarchyDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetHierarchyDsl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.property

internal class KotlinTargetHierarchyDslImpl(
    objects: ObjectFactory,
    private val targets: DomainObjectCollection<KotlinTarget>,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) : KotlinTargetHierarchyDsl {

    private val _appliedDescriptors = mutableListOf<KotlinTargetHierarchyDescriptor>()
    val appliedDescriptors: List<KotlinTargetHierarchyDescriptor> get() = _appliedDescriptors

    override val android: KotlinAndroidTargetHierarchyDsl = KotlinAndroidTargetHierarchyDslImpl(objects)

    override fun apply(
        hierarchyDescriptor: KotlinTargetHierarchyDescriptor,
        describeExtension: (KotlinTargetHierarchyBuilder.Root.() -> Unit)?,
    ) {
        val descriptor = hierarchyDescriptor.extendIfNotNull(describeExtension)
        _appliedDescriptors.add(descriptor)
        applyKotlinTargetHierarchy(descriptor, targets, sourceSets)
    }

    override fun default(describeExtension: (KotlinTargetHierarchyBuilder.Root.() -> Unit)?) {
        apply(defaultKotlinTargetHierarchy, describeExtension)
    }

    override fun custom(describe: KotlinTargetHierarchyBuilder.Root.() -> Unit) {
        apply(KotlinTargetHierarchyDescriptor(describe))
    }

    override fun android(configure: KotlinAndroidTargetHierarchyDsl.() -> Unit) {
        android.configure()
    }
}

private fun KotlinTargetHierarchyDescriptor.extendIfNotNull(describe: (KotlinTargetHierarchyBuilder.Root.() -> Unit)?) =
    if (describe == null) this else extend(describe)

internal class KotlinAndroidTargetHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidTargetHierarchyDsl {
    override val main: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val unitTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val instrumentedTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
}

internal class KotlinAndroidVariantHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidVariantHierarchyDsl {
    override val sourceSetTree: Property<KotlinTargetHierarchy.SourceSetTree> = objects.property()
}