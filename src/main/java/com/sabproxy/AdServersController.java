package com.sabproxy;

import com.sabproxy.util.AdServers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdServersController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/adservers/update", method = RequestMethod.GET)
    @ResponseBody
    public String updateAdServers(Model model) {

        AdServers adServers = SABPServer.getAdServers();
        boolean updated = adServers.downloadAdServersList();
        adServers.loadListFromHostsFileFormat(null);

        if (!updated) {
            return "Something went wrong. Could not update ad servers list.";
        }
        return "Successfully updated. <br/>Loaded "+adServers.getNumberOfLoadedAdServers()+" ad servers.";

    }

}
