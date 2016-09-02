package net.vivin.gradle.versioning

import org.testng.annotations.Test

import static org.testng.Assert.assertFalse

class VersionUtilsTests extends TestNGRepositoryTestCase {
    @Test
    void testTaggedVersionIsRecognizedAsNonSnapshot() {
        testRepository.commitAndTag("0.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()

        version.versionUtils.determineVersion()
        assertFalse(version.snapshot, "Tagged version should not be snapshot version")
    }
}