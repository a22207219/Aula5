package pt.ulusofona.cd.order.config;
import org.springframework.context.annotation.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.web.client.RestClient;import org.springframework.web.client.support.RestClientAdapter;import org.springframework.web.service.invoker.HttpServiceProxyFactory;import org.springframework.http.client.SimpleClientHttpRequestFactory;import pt.ulusofona.cd.order.client.*;import java.time.Duration;
@Configuration public class HttpClientConfig {
private RestClient client(String url){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofSeconds(2));f.setReadTimeout(Duration.ofSeconds(3));return RestClient.builder().baseUrl(url).requestFactory(f).build();}
private <T> T proxy(String url,Class<T> type){return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client(url))).build().createClient(type);}
@Bean ProductClient productClient(@Value("${services.product-base-url}") String url){return proxy(url,ProductClient.class);}
@Bean SupplierClient supplierClient(@Value("${services.supplier-base-url}") String url){return proxy(url,SupplierClient.class);}
}
