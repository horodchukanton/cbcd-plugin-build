import com.cloudbees.cd.plugins.build.allure.client.AllureServiceClient
import spock.lang.Specification

class AllureServiceClientTest extends Specification {

    static final URL = 'http://localhost:5050/allure-service-docker'

    def constructor() {
        given:
        def client = new AllureServiceClient(URL)
        expect:
        client instanceof AllureServiceClient
    }

    def get() {
        given:
        String projectsPath = '/projects'
        String projectName = 'default'
        def client = new AllureServiceClient(URL)

        when:
        def resp = client.isProjectExists(projectName)

        then:
        assert resp == true
    }

    def listProjects() {
        given:
        String projectsPath = '/projects'
        String projectName = 'default'
        def client = new AllureServiceClient(URL)

        when:
        def resp = client.get(projectsPath)
        Map<String, Object> data = resp['data']['projects']
        then:
        assert data.containsKey(projectName)
    }

    def post() {
        given:
        String projectsPath = '/projects'
        String projectName = UUID.randomUUID().toString().toLowerCase()
        String json = '{"id":"' + projectName + '"}'

        def client = new AllureServiceClient(URL)

        when:
        def resp = client.post(projectsPath, json)
        String created = resp['data']['id']
        then:
        assert created == projectName
    }

    def query() {
        given:
        String projectsPath = '/generate-report'
        String projectName ='default'

        def client = new AllureServiceClient(URL)

        when:
        def resp = client.get(projectsPath, [projectName: projectName])
        String created = resp['data']['report_url']
        then:
        assert created != null
    }

}
