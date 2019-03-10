import spock.lang.Specification
import spock.lang.Unroll

/**
 * Attempt at unit testing device handler. Smartthings doesn't make it easy...
 * This will read XiaomiMijiaButtonutton.groovy as plain text and remove everything above '// METHODS START' line
 *  which is not proper groovy syntax. Then using GroovyShell, it makes a `Script` available which by
 *  using `invokeMethod` you can call methods on the file.
 */
@Unroll
class XiaomiMijiaButtonSpec extends Specification {

    Script xiaomiButton

    def setup() {
        String fileUnderTest = new File('src/main/groovy/XiaomiMijiaButton.groovy').text
        def index = fileUnderTest.indexOf('// METHODS START')
        def xiaomiButtonClassString = fileUnderTest.substring(index)
        xiaomiButton = new GroovyShell(defaultBinding()).parse(xiaomiButtonClassString)
    }

    def "Should convert parse message into IncomingEvent enum type"() {
        expect:
            xiaomiButton.invokeMethod("resolveIncomingEvent", parseMessage) == messageValue
        where:
            parseMessage                                                        | messageValue
            'on/off: 0'                                                         | 'BUTTON_1'
            'on/off: 1'                                                         | 'BUTTON_1_RELEASED'
            'catchall: 0104 0006 01 01 0100 00 491B 00 00 0000 0A 01 00802002'  | 'BUTTON_2'
            'catchall: 0104 0006 01 01 0100 00 491B 00 00 0000 0A 01 00802003'  | 'BUTTON_3'
            'catchall: 0104 0006 01 01 0100 00 491B 00 00 0000 0A 01 00802004'  | 'BUTTON_4'
            'catchall: 0104 0006 01 01 0100 00 491B 00 00 0000 0A 01 008020002' | 'CATCH_ALL'
            'read attr - raw: foo bar'                                          | 'READ_ATTR'
            'foo bar'                                                           | 'UNKNOWN'
    }

    def defaultBinding() {
        new Binding(
                debugLogging: false,
                infoLogging : false,
        )
    }

}
