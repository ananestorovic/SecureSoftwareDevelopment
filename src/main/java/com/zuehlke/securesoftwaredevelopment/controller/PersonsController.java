package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.PersonRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller

public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    public String person(@PathVariable int id, Model model, HttpSession session) throws AccessDeniedException {

        if(!SecurityUtil.hasPermission("VIEW_PERSON")){
            int currentUserId = SecurityUtil.getCurrentUser().getId();
            if(currentUserId != id){
                throw new AccessDeniedException("Forbidden");
            }
        }
        String csrfToken = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", csrfToken);
        model.addAttribute("person", personRepository.get("" + id));
        return "person";
    }

    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')")
    public String self(Model model, Authentication authentication, HttpSession session) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("person", personRepository.get("" + user.getId()));

        String csrfToken = session.getAttribute("CSRF_TOKEN").toString();
        model.addAttribute("CSRF_TOKEN", csrfToken);
        return "person";
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> person(@PathVariable int id) throws AccessDeniedException {

        if(!SecurityUtil.hasPermission("UPDATE_PERSON")){
            int currentUserId = SecurityUtil.getCurrentUser().getId();
            if(currentUserId != id){
                LOG.error("Access denied. User ID " + currentUserId + " does not have permission to update person details.");
                throw new AccessDeniedException("Access denied.");
            }
        }

        personRepository.delete(id);
        userRepository.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-person")
    public String updatePerson(Person person, HttpSession session, @RequestParam("csrfToken") String csrfToken) throws
            AccessDeniedException {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        if (!csrf.equals(csrfToken)) {
            throw new AccessDeniedException("Access denied.");
        }

        if(!SecurityUtil.hasPermission("UPDATE_PERSON")){
            int currentUserId = SecurityUtil.getCurrentUser().getId();
            int personId = Integer.parseInt(person.getId());
            if(currentUserId != personId){
                LOG.error("Access denied. User ID " + currentUserId + " does not have permission to update person details.");
                throw new AccessDeniedException("Access denied.");
            }
        }
        personRepository.update(person);
        return "redirect:/persons/" + person.getId();
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public String persons(Model model) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}
