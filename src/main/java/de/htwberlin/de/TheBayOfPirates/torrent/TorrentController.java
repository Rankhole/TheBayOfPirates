package de.htwberlin.de.TheBayOfPirates.torrent;

import de.htwberlin.de.TheBayOfPirates.rating.UserRatingService;
import de.htwberlin.de.TheBayOfPirates.registration.RegistrationController;
import de.htwberlin.de.TheBayOfPirates.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class TorrentController {

    @Autowired
    TorrentService torrentService;

    @Autowired
    UserService userService;

    @Autowired
    UserRatingService userRatingService;

    public TorrentController(UserService userService, TorrentService torrentService, UserRatingService userRatingService) {
        this.torrentService = torrentService;
        this.userRatingService = userRatingService;
    }

    @GetMapping(value = "/torrent/name={name:.+}")
    public ModelAndView getTorrent(@PathVariable String name, Principal principal) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        RegistrationController.handleSecurity(modelAndView, principal, userService);
        Optional<Torrent> torrent = torrentService.findByName(name);
        if (torrent.isPresent()) {
            modelAndView.addObject("torrent", torrent.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Torrent not found!");
        }
        modelAndView.setViewName("showtorrent");
        modelAndView.addObject("previousRating", userRatingService.getPreviousUSerRatingOfTorrentByName(name, principal.getName()));
        return modelAndView;
    }

    @GetMapping(value = "/torrent/id={id}")
    public ModelAndView getTorrent(@PathVariable int id, Principal principal) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        RegistrationController.handleSecurity(modelAndView, principal, userService);
        Optional<Torrent> torrent = torrentService.findByTorrentID(id);
        if (torrent.isPresent()) {
            modelAndView.addObject("torrent", torrent.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Torrent not found!");
        }
        modelAndView.setViewName("showtorrent");
        modelAndView.addObject("previousRating", userRatingService.getPreviousUserRatingOfTorrent(id, principal.getName()));
        return modelAndView;
    }

    @GetMapping(value = "/torrent/upload")
    public ModelAndView postTorrent(Principal principal) {
        ModelAndView modelAndView = new ModelAndView();
        RegistrationController.handleSecurity(modelAndView, principal, userService);
        modelAndView.addObject("description", "");
        modelAndView.setViewName("torrentUpload");
        return modelAndView;
    }

    @PostMapping(value = "/torrent/upload")
    public ModelAndView postTorrent(@RequestParam("file") MultipartFile multipartFile,
                                    Principal principal, @RequestParam("description") String description) {
        ModelAndView modelAndView = new ModelAndView();
        RegistrationController.handleSecurity(modelAndView, principal, userService);
        String fileName = multipartFile.getOriginalFilename();
        String actualName = fileName.substring(0, fileName.length() - ".torrent".length());
        if (torrentService.findByName(actualName).isPresent()) {
            modelAndView.addObject("error",
                    "Torrent with that name already exists!");
            modelAndView.setViewName("redirect:/torrent/upload");
        } else {
            try {
                Torrent savedTorrent = torrentService.saveTorrentBytes(multipartFile.getBytes(), fileName,
                        principal.getName(), description);
                modelAndView.addObject("successMessage", "Upload succeeded!");
                modelAndView.setViewName("redirect:/torrent/id=" + savedTorrent.getTorrentID());
            } catch (Exception e) {
                e.printStackTrace();
                modelAndView.addObject("error", "Upload failed!");
                modelAndView.setViewName("redirect:/torrent/upload");
            }
        }

        return modelAndView;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        System.out.println(name + " parameter is missing");
        return "/torrent/upload";
    }

    @GetMapping(value = "/torrent/download")
    public ResponseEntity<InputStreamResource> downloadFile1(@RequestParam(name = "filename") String filename) throws Exception {
        Optional<Torrent> torrent = torrentService.findByName(filename);
        if (!torrent.isPresent()) {
            throw new Exception("Torrent not found! Shouldn't have happened!");
        }
        byte[] torrentBytes = torrent.get().getTorrent();
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(torrentBytes));
        String actualName = filename + ".torrent";
        return ResponseEntity.ok()
                // Content-Disposition
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + actualName)
                // Contet-Length
                .contentLength(torrentBytes.length)
                .body(resource);
    }

    @PostMapping(value = "/torrent/delete")
    public ModelAndView deleteTorrent(@RequestParam(name = "filename") String filename, Principal principal) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        Optional<Torrent> torrent = torrentService.findByName(filename);
        if (torrent.isPresent()) {
            if (principal.getName().equals(torrent.get().getUser().getEmail())) {
                torrentService.removeTorrentByName(filename);
                modelAndView.addObject("successMessage", "Torrent \"" + torrent.get().getName() + "\"deleted successfully!");
                modelAndView.setViewName("redirect:/torrent/upload");
                return modelAndView;
            } else {
                throw new Exception("Not the original uploader!");
            }
        } else {
            throw new Exception("Torrent not found");
        }
    }

    @GetMapping(value = "/torrent/search")
    public ModelAndView searchTorrent(@RequestParam(name = "search") String search, @RequestParam(defaultValue = "10", name = "size") Optional<Integer> size) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/torrent/search=" + search + "/page=1" + (size.map(integer -> "?size=" + integer).orElse("")));
        return modelAndView;
    }

    @GetMapping(value = "/torrent/search={searchTerm}/page={page}")
    public ModelAndView searchForTorrentByName(@PathVariable String searchTerm, @PathVariable @Min(1) int page, Principal principal, @RequestParam(defaultValue = "10", name = "size") @Min(1) @Max(100) Optional<Integer> size) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        RegistrationController.handleSecurity(modelAndView, principal, userService);
        Page<Torrent> torrentPage;
        if (size.isPresent()) {
            torrentPage = torrentService.getTorrentPagesBySearch(searchTerm, page - 1, size.get()); //-1 because it starts at 0
        } else {
            throw new Exception("Size not specified! Error!");
        }
        modelAndView.addObject("totalPages", torrentPage.getTotalPages());
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("isFirstPage", page == 1);
        modelAndView.addObject("isLastPage", page == torrentPage.getTotalPages());
        modelAndView.addObject("searchURL", "/torrent/search=" + searchTerm + "/page=");
        modelAndView.addObject("torrentPage", torrentPage);
        if (size.get() != 10)
            modelAndView.addObject("sizeRequest", +size.get());

        int totalPages = torrentPage.getTotalPages();
        // Get total number of pages
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            modelAndView.addObject("pageNumbers", pageNumbers);
        }

        if (torrentPage.getTotalElements() == 0) {
            modelAndView.addObject("successMessage", "No Torrents with that name found!");
        }
        modelAndView.setViewName("search");
        return modelAndView;
    }
}
