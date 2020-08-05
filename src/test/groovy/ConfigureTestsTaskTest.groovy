import com.cloudbees.cd.plugins.build.specs.ConfigureTestTask
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ConfigureTestsTaskTest extends Specification{


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

        File envFile = new File(this.class.getResource("systemtest.env").toURI())

        then:
        Map<String, String> env = conf.readEnvironmentFrom(envFile)
        assert env['SECRET'] == "value"
    }

    def "Check secret resolved"(){
        when:
        def project = ProjectBuilder.builder().build()
        def conf = project.task('configureTestsTask', type: ConfigureTestTask)

        File secretFile = new File(this.class.getResource("remote-secrets.env").toURI())

        then:
        Map<String, String> secrets = conf.readEnvironmentFrom(secretFile)
        assert secrets['USER'] != null

        String resolved = conf.resolveSecret(secrets['USER'])
        assert  resolved == "admin"
    }

}
