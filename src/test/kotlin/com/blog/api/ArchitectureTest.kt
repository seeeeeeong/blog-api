package com.blog.api

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val classes = ClassFileImporter().importPackages("com.blog.api")

    @Test
    fun `domain should not depend on controller`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .check(classes)
    }

    @Test
    fun `storage should not depend on controller`() {
        noClasses()
            .that().resideInAPackage("..storage..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .check(classes)
    }

    @Test
    fun `controller should not depend on storage directly`() {
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..storage..")
            .check(classes)
    }
}
