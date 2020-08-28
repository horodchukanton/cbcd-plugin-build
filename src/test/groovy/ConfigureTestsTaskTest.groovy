import com.cloudbees.cd.plugins.build.specs.ConfigureTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ConfigureTestsTaskTest extends Specification{

    static String defaultResourcesLocation = '/environments/default'
    static String extraEnvironmentsLocation = 'extra/location/for'

    def "Check gcloud executable resolved"(){
        when:
        def path = ConfigureTestTask.findDirForProgram('gcloud')
        then:
        println(path)
        assert path != null
    }

    def "Check secret is read"(){
        when:
        def project = ProjectBuilder.builder().build()
        def conf = project.task('configureTestsTask', type: ConfigureTestTask)

        File envFile = new File(this.class.getResource("${defaultResourcesLocation}/systemtest.env").toURI())

        then:
        Map<String, String> env = conf.readEnvironmentFrom(envFile)
        assert env['SECRET'] == "value"
    }

    def "Check secret resolved"(){
        when:
        def project = ProjectBuilder.builder().build()
        def conf = project.task('configureTestsTask', type: ConfigureTestTask)

        File secretFile = new File(this.class.getResource("${defaultResourcesLocation}/remote-secrets.env").toURI())

        then:
        Map<String, String> secrets = conf.readEnvironmentFrom(secretFile)
        assert secrets['USER'] != null

        String resolved = conf.resolveSecret(secrets['USER'])
        assert  resolved == "admin"
    }

    def "Check files from usual location are read"(){
        given:
        def project = ProjectBuilder.builder().build()

        // Create a file in a project root
        File projectDir = project.getProjectDir()
        File envDir = new File(projectDir, defaultResourcesLocation)
        envDir.mkdirs()
        File envFile = new File(envDir, 'systemtest.env')
        envFile.write("SIMPLE_VALUE=simple_value\n")

        project.task('test', type: Test)

        ConfigureTestTask conf = project.task('configureTestsTask', type: ConfigureTestTask) as ConfigureTestTask

        when:
        conf.configureProject()

        then:
        Test test = project.getTasksByName('test', false).first() as Test
        assert test.getEnvironment().get('SIMPLE_VALUE').equals('simple_value')

    }

    def "Check files from extra location are read"(){
        given:
        def project = ProjectBuilder.builder().build()
        project.task('test', type: Test)

        ConfigureTestTask conf = project.task('configureTestsTask', type: ConfigureTestTask) as ConfigureTestTask
        conf.environmentsLocation = new File(this.class.getResource(extraEnvironmentsLocation).toURI())

        when:
        conf.configureProject()

        then:
        Test test = project.getTasksByName('test', false).first() as Test
        assert test.getEnvironment().get('SIMPLE_VALUE').equals('simple_value')

    }

}
