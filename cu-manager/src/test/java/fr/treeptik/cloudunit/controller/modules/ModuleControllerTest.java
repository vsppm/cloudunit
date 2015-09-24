/*
 * LICENCE : CloudUnit is available under the Gnu Public License GPL V3 : https://www.gnu.org/licenses/gpl.txt
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.controller.modules;

import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.ApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import junit.framework.TestCase;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;
import javax.servlet.Filter;
import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by nicolas on 08/09/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        ApplicationContext.class,
        MockServletContext.class
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModuleControllerTest extends TestCase {

    private static String SEC_CONTEXT_ATTR = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    private final Logger logger = LoggerFactory
            .getLogger(ModuleControllerTest.class);

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Inject
    private AuthenticationManager authenticationManager;

    @Autowired
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;

    private Authentication authentication;
    private MockHttpSession session;

    private static String applicationName;

    @BeforeClass
    public static void initEnv() {
        applicationName = "App"+new Random().nextInt(1000);
    }

    @Before
    public void setup() {
        logger.info("setup");

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        User user = null;
        try {
            user = userService.findByLogin("johndoe");
        } catch (ServiceException e) {
            logger.error(e.getLocalizedMessage());
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getLogin(), user.getPassword());
        Authentication result = authenticationManager.authenticate(authentication);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(result);
        session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
    }

    @After
    public void teardown() {
        logger.info("teardown");

        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test
    public void test010_CreateApplication() throws Exception {
        logger.info("test01_CreateApplication");

        long startTime = System.currentTimeMillis();
        logger.info("Create Tomcat server");

        final String jsonString = "{\"applicationName\":\""+applicationName
                + "\", \"serverName\":\"tomcat-8\"}";
        try {
            ResultActions resultats = this.mockMvc
                    .perform(
                            post("/application").session(session)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonString));
            resultats.andExpect(status().isOk());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        long endTime = System.currentTimeMillis();
        logger.info("End (timex:" + (endTime - startTime) + " millis).");
    }

    /**
     * We cannot create an application with an empty name.
     * @throws Exception
     */
    @Test
    public void test011_FailCreateEmptyNameApplication() throws Exception {
        logger.info("test01_CreateApplication");

        long startTime = System.currentTimeMillis();
        logger.info("Create Tomcat server");

        final String jsonString = "{\"applicationName\":\", \"serverName\":\"tomcat-8\"}";
        try {
            ResultActions resultats = this.mockMvc
                    .perform(
                            post("/application").session(session)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonString));
            resultats.andExpect(status().is4xxClientError()).andDo(print());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        long endTime = System.currentTimeMillis();
        logger.info("End (timex:" + (endTime - startTime) + " millis).");
    }

    @Test
    public void test02_StopApplicationTest() throws ServiceException {

        final String jsonString = "{\"applicationName\":\""+applicationName+"\"}";
        try {
            this.mockMvc
                    .perform(
                            post("/application/stop").session(session)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonString))
                    .andExpect(status().isOk());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new ServiceException("Error test02StopApplicationTest", ex);
        }
    }

    @Test
    public void test03_StartApplicationTest() throws ServiceException {

        final String jsonString = "{\"applicationName\":\""+applicationName+"\"}";
        try {
            this.mockMvc
                    .perform(
                            post("/application/start").session(session)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonString))
                    .andExpect(status().isOk());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new ServiceException("Error test02StopApplicationTest", ex);
        }
    }

    @Test
    public void test04_DeleteApplication() throws Exception {
        logger.info("test02_deleteApplication");

        long startTime = System.currentTimeMillis();
        logger.info("Delete Tomcat server");
        try {
            ResultActions resultats = this.mockMvc
                    .perform(
                            delete("/application/" + applicationName).session(session)
                                    .contentType(MediaType.MULTIPART_FORM_DATA));
            resultats.andExpect(status().isOk());
        } catch (Exception ex) {
            throw new ServiceException(ex.getMessage());
        }
        long endTime = System.currentTimeMillis();
        logger.info("End (timex:" + (endTime - startTime) + " millis).");
    }


}