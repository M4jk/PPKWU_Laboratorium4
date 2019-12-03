package com.i209344;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Revision;
import ezvcard.property.StructuredName;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CardController {

    @GetMapping("/")
    public String main(Model model) {
        UserSearch userSearch = new UserSearch();
        userSearch.setName("");
        model.addAttribute("userSearch", userSearch);
        return "index";
    }

    @RequestMapping(value = "/cards", method = RequestMethod.POST)
    public String search(@ModelAttribute UserSearch userSearch, Model model) throws IOException {
        model.addAttribute("userInputSearch", userSearch);
        String calendarEndpoint="https://adm.edu.p.lodz.pl/user/users.php?search=" + userSearch.name;

        Document document = Jsoup.connect(calendarEndpoint).get();
        List<Person> people = new ArrayList<>();

        Elements segment = document.select("div.user-info");
        for (Element element : segment) {
            Person person = new Person();

            if (!element.select("h3").text().equals("")) {
                person.setName(element.select("h3").text());
            }
            if (!element.select("h4").text().equals("")) {
                person.setTitle(element.select("h4").text());
            }
            if (!element.select("span.item-content").text().equals("")) {
                person.setWorkingPlace(element.select("span.item-content").text());
            }

            people.add(person);
        }

        model.addAttribute("employees", people);
        return "cards";
    }

    private Person fromString(String string) {
        Person person = new Person();

        String[] splitted = string.split("&");
        String name = splitted[0].replace("{", "").replace("}", "");
        person.setName(name);

        String title = splitted[1].replace("{", "").replace("}", "");
        if (!title.equals("null")) {
            person.setTitle(title);
        }

        String workingPlace = splitted[2].replace("{", "").replace("}", "");
        person.setWorkingPlace(workingPlace);

        return person;
    }

    @RequestMapping(value = "/vcard/{employee}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String employee, Model model) throws IOException {
        VCard vcard = new VCard();

        Person person = fromString(employee);

        StructuredName n = new StructuredName();
        n.setFamily(person.name.split(" ")[1]);
        n.setGiven(person.name.split(" ")[0]);
        if (person.title != null) {
            vcard.addTitle(person.title);
        }

        vcard.setFormattedName(person.getName());
        vcard.setRevision(Revision.now());

        File vcardFile = new File("vcard.vcf");
        Ezvcard.write(vcard).version(VCardVersion.V4_0).go(vcardFile);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vcard.vcf");
        Resource fileSystemResource = new FileSystemResource("vcard.vcf");
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileSystemResource);
    }

    private class UserSearch {
        String name;

        public UserSearch() { }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private class Person {
        String name;
        String title;
        String workingPlace;

        public Person () {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getWorkingPlace() {
            return workingPlace;
        }

        public void setWorkingPlace(String workingPlace) {
            this.workingPlace = workingPlace;
        }

        @Override
        public String toString() {
            return "{" + name + "}&{" + title + "}&{" + workingPlace + "}";
        }
    }

}

