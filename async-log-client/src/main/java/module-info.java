import javax.annotation.processing.Processor;
import org.example.async.log.client.annotation.AsyncLogProcessor;

module org.example.async.log.client{
    provides Processor with AsyncLogProcessor;
    requires static jdk.compiler;
    requires static java.compiler;
}