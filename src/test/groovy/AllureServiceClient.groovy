import com.cloudbees.cd.plugins.build.allure.client.AllureServiceClient
import spock.lang.Specification

class AllureServiceClientTest extends Specification {

    static final URL = 'http://localhost:5050/allure-service-docker'
    static final String projectName = UUID.randomUUID().toString().toLowerCase()

    def constructor() {
        given:
        def client = new AllureServiceClient(URL)
        expect:
        client instanceof AllureServiceClient
    }

    def checkDefaultExists() {
        given:
        String projectName = 'default'
        def client = new AllureServiceClient(URL)

        when:
        def resp = client.isProjectExists(projectName)

        then:
        assert resp
    }

    def check404() {
        given:
        def client = new AllureServiceClient(URL)
        String projectName = UUID.randomUUID().toString().toLowerCase()

        expect:
        !client.isProjectExists(projectName)
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

    def get() {
        given:
        def client = new AllureServiceClient(URL)
        String projectName = UUID.randomUUID().toString().toLowerCase()
        client.createProject(projectName)

        when:
        def project = client.get('/clean-history', [project_id: projectName])

        then:
        assert project['meta_data']['message'] == "History successfully cleaned for project_id '$projectName'"
    }

    def post() {
        given:
        String projectsPath = '/projects'
        String json = '{"id":"' + projectName + '"}'

        def client = new AllureServiceClient(URL)

        when:
        def resp = client.post(projectsPath, json)
        String created = resp['data']['id']
        then:
        assert created == projectName
        assert client.isProjectExists(projectName)
    }

    def query() {
        given:
        String projectsPath = '/generate-report'
        String projectName = 'default'

        def client = new AllureServiceClient(URL)

        when:
        def resp = client.get(projectsPath, [project_id: projectName])
        String created = resp['data']['report_url']
        then:
        assert created != null
    }

    def autoCreation() {
        given:
        def client = new AllureServiceClient(URL)
        String projectName = UUID.randomUUID().toString().toLowerCase()

        if (!client.isProjectExists(projectName)) {
            println "Creating project " + projectName
            client.createProject(projectName)
        }
    }

    def isServerAccessible() {
        given:
        def valid_url = new AllureServiceClient(URL)
//        def invalid_url = new AllureServiceClient('whatever')
        def wrong_url = new AllureServiceClient('https://google.com:5050')
        def inaccessible_url = new AllureServiceClient('https://10.0.0.1:5050')

        expect:
        valid_url.isServerAccessible()
//        !invalid_url.isServerAccessible()
        !wrong_url.isServerAccessible()
        !inaccessible_url.isServerAccessible()
    }

}
