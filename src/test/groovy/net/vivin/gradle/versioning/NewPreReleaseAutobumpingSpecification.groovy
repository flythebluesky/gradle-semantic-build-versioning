package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

@Title('New Pre-Release Autobumping Specification')
class NewPreReleaseAutobumpingSpecification extends Specification {
    private TestRepository testRepository

    @Subject
    private SemanticBuildVersion semanticBuildVersion

    def setup() {
        def project = Mock(Project) {
            getProjectDir() >> testRepository.repository.workTree
        }
        semanticBuildVersion = new SemanticBuildVersion(project)
        semanticBuildVersion.with {
            config = new SemanticBuildVersionConfiguration()
            config.preRelease = new PreRelease(startingVersion: 'pre.0')
            config.preRelease.bump = {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
            snapshot = false
        }
    }

    @Unroll
    def 'new pre-release autobumping with #testNamePart causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [new-pre-release]
                [$autobumpTag]
            """.stripIndent()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == expectedExceptionMessage

        where:
        testNamePart         | autobumpTag   || expectedExceptionMessage
        'pre-release bump'   | 'pre-release' || 'Bumping pre-release component while also creating a new pre-release is not supported'
        'promote to release' | 'promote'     || 'Creating a new pre-release while also promoting a pre-release is not supported'
    }

    @Unroll('#testName')
    def 'test various new pre-release autobumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it, annotated
        }
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [new-pre-release]
                [$autobumpTag]
            """.stripIndent()

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        tagNames  | autobumpTag | annotated || expectedVersion
        []        | false       | false     || '0.1.0-pre.0'
        []        | 'patch'     | false     || '0.1.0-pre.0'
        []        | 'minor'     | false     || '0.2.0-pre.0'
        []        | 'major'     | false     || '1.0.0-pre.0'
        ['0.2.0'] | false       | false     || '0.2.1-pre.0'
        ['0.2.0'] | 'patch'     | false     || '0.2.1-pre.0'
        ['0.2.0'] | 'minor'     | false     || '0.3.0-pre.0'
        ['0.2.0'] | 'major'     | false     || '1.0.0-pre.0'
        []        | false       | true      || '0.1.0-pre.0'
        []        | 'patch'     | true      || '0.1.0-pre.0'
        []        | 'minor'     | true      || '0.2.0-pre.0'
        []        | 'major'     | true      || '1.0.0-pre.0'
        ['0.2.0'] | false       | true      || '0.2.1-pre.0'
        ['0.2.0'] | 'patch'     | true      || '0.2.1-pre.0'
        ['0.2.0'] | 'minor'     | true      || '0.3.0-pre.0'
        ['0.2.0'] | 'major'     | true      || '1.0.0-pre.0'

        and:
        testName = "new pre-release autobumping ${tagNames ? 'with' : 'without'} prior versions " +
            (autobumpTag ? "with bump $autobumpTag" : 'and without explicit bump') + " (annotated: $annotated)"
    }
}