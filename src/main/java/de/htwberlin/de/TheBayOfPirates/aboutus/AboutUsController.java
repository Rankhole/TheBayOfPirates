package de.htwberlin.de.TheBayOfPirates.aboutus;

import de.htwberlin.de.TheBayOfPirates.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

@Controller
public class AboutUsController {

    @Autowired
    private DateAndTime dateAndTime;


    /**
     * Request Mapping for /about (About Page). We fill the localdatetime with the information from our model.
     * @param model model
     * @return AboutUs the view which is to be returned

    @RequestMapping({"/about"})
    public String dateAndTime(Model model) {
        model.addAttribute("localdatetime", dateAndTime.getDateAndTime());
        return "AboutUs";
    }
    */


    @GetMapping(value = "/about")
    public ModelAndView getAboutUs(Principal principal, Model model){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("AboutUs"); // resources/template/home.html
        modelAndView.addObject("user", new User());
        modelAndView.addObject("principal", principal);
        model.addAttribute("localdatetime", dateAndTime.getDateAndTime());
        return modelAndView;
    }
}
