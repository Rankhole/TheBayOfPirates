package de.htwberlin.de.TheBayOfPirates.torrent;

import de.htwberlin.de.TheBayOfPirates.user.User;
import de.htwberlin.de.TheBayOfPirates.user.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.springframework.data.domain.Page;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


class TorrentServiceImplTest {

    private TorrentRepository torrentRepository;
    private UserService userService;
    private byte[] torrentAsByte;
    private User mockedUser;
    private final String description = "This is a test description ........................";
    private final String filename = "archlinux-2019.12.01-x86_64.iso";
    private TorrentServiceImpl torrentService;
    private File torrentFile;
    private Torrent torrent;

    @BeforeEach
    void setUpMocks() throws IOException {
        torrentRepository = Mockito.mock(TorrentRepository.class);
        userService = Mockito.mock(UserService.class);
        ClassLoader classLoader = getClass().getClassLoader();
        torrentFile = new File(classLoader.getResource("archlinux-2019.12.01-x86_64.iso.torrent").getFile());
        torrentAsByte = Files.readAllBytes(torrentFile.toPath());
        mockedUser = Mockito.mock(User.class);
        Mockito.when(mockedUser.getEmail()).thenReturn("muhammed@gmail.com");
        Mockito.when(mockedUser.getName()).thenReturn("Muhammed");
        Mockito.when(mockedUser.getSurname()).thenReturn("Geldi");
        Mockito.when(mockedUser.getPassword()).thenReturn("wohohhhh");
        Mockito.when(mockedUser.getUsername()).thenReturn("Slayer");
        Mockito.when(userService.findByUserEmail(Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(userService.findByUserEmail("muhammed@gmail.com")).thenReturn(Optional.of(mockedUser));
        torrent = Mockito.mock(Torrent.class);
        Mockito.when(torrent.getDescription()).thenReturn(description);
        Mockito.when(torrent.getName()).thenReturn(filename);
        Mockito.when(torrent.getTorrent()).thenReturn(torrentAsByte);
        Mockito.when(torrentRepository.findByTorrentID(Mockito.anyInt())).thenReturn(Optional.empty());
        Mockito.when(torrentRepository.findByName(filename)).thenReturn(Optional.of(torrent));
        Mockito.when(torrentRepository.findByName("something")).thenReturn(Optional.empty());
        Mockito.when(torrentRepository.findAllByNameContaining(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mockito.mock(Page.class));
        torrentService = new TorrentServiceImpl(torrentRepository, userService);
        //we need to delete the previous test run files
        File torrentFile = new File(System.getProperty("user.home") + "/" + filename + ".torrent");
        torrentFile.delete();
        File torrentFileSomething = new File(System.getProperty("user.home") + "/" + "something" + ".torrent");
        torrentFileSomething.delete();
    }

    @Test
    void saveTorrent() {
        try {
            torrentService.saveTorrentBytes(torrentAsByte, torrentFile.getName(), mockedUser.getEmail(), description);
            Mockito.verify(userService, Mockito.times(1))
                    .findByUserEmail("muhammed@gmail.com");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void saveTorrentWithNonExistingUser() {
        Mockito.when(userService.findByUserEmail(Mockito.any())).thenReturn(Optional.empty());
        try {
            torrentService.saveTorrentBytes(torrentAsByte, torrentFile.getName(), "someNonExistingUser", description);
            fail("Expected an exception!");
        } catch (Exception e) {
            assertEquals("User not found!", e.getMessage());
        }
    }

    @Test
    void testRepositoryCalls() {
        torrentService.findByName(filename);
        torrentService.findByTorrentID(1);
        Mockito.verify(torrentRepository, Mockito.times(1)).findByName(filename);
        Mockito.verify(torrentRepository, Mockito.times(1)).findByTorrentID(1);
    }

    @Test
    void testSaveTorrentByBytes() throws Exception {
        Torrent returnedTorrent = torrentService.saveTorrentBytes(torrentAsByte, torrentFile.getName(), mockedUser.getEmail(), description);
        Mockito.verify(userService, Mockito.times(1)).findByUserEmail(mockedUser.getEmail());
        Mockito.verify(torrentRepository, Mockito.times(1)).save(Mockito.any());
        assertEquals(torrent.getDescription(), returnedTorrent.getDescription());
        assertEquals(torrent.getName(), returnedTorrent.getName());
        assertEquals(torrent.getTorrent(), returnedTorrent.getTorrent());
        assertEquals(torrent.getTorrentID(), returnedTorrent.getTorrentID());
    }

    @Test
    void getTorrentPagesBySearch() {
        Object resultPage = torrentService.getTorrentPagesBySearch("someSearchTerm", 0, 10);
        MockingDetails mockingDetails = Mockito.mockingDetails(resultPage);
        Assert.assertTrue(new ReflectionEquals(Page.class).matches(mockingDetails.getMockCreationSettings().getTypeToMock()));
    }
}