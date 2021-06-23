
package video.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//@FeignClient(name="pay", url="http://pay:8080")
@FeignClient(name="pay", url="${api.pay.url}")
public interface PayService {

    @RequestMapping(method= RequestMethod.POST, path="/pays")
    public void payment(@RequestBody Pay pay);

    
}