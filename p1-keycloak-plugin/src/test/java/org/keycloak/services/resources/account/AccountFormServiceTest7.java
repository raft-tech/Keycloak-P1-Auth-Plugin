package org.keycloak.services.resources.account;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.core.ResteasyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.account.AccountPages;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.services.managers.Auth;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.userprofile.UserProfileProvider;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AccountFormServiceTest7 {

    /**
     * TestableAccountFormService is used for most tests.
     */
    private static class TestableAccountFormService extends AccountFormService {
        public TestableAccountFormService(KeycloakSession session, ClientModel client, EventBuilder eventBuilder) {
            super(session, client, eventBuilder);
        }
        @Override
        public void init() {
            // Skip initialization to avoid NullPointerExceptions
        }
        @Override
        protected Response login(String path) {
            return Response.ok().build();
        }
    }

    @Mock private KeycloakSession keycloakSession;
    @Mock private org.keycloak.models.KeycloakContext keycloakContext;
    @Mock private RealmModel realmModel;
    @Mock private ClientModel clientModel;
    @Mock private KeycloakUriInfo keycloakUriInfo;
    @Mock private AccountProvider accountProvider;
    @Mock private EventBuilder eventBuilder;
    @Mock private HttpHeaders dummyHeaders;
    @Mock private CookieProvider cookieProvider;
    @Mock private UserProfileProvider userProfileProvider;
    @Mock private EventStoreProvider eventStoreProvider;
    @Mock private UserSessionProvider userSessionProvider;
    @Mock private ClientConnection clientConnection;
    @Mock private HttpRequest dummyRequest;
    @Mock private LoginFormsProvider loginFormsProvider;
    @Mock private UserModel dummyUser;
    @Mock private UserProvider userProvider;
    @Mock private AuthenticationSessionProvider authSessionProvider;

    // Base URI for tests.
    private final URI baseUri = URI.create("http://example.com");

    private TestableAccountFormService testService;
    private Auth dummyAuth;

    // Static mock for Profile
    private MockedStatic<Profile> profileMock;

    /**
     * Force-set a non-static, non-final field by its exact name.
     */
    private static void forceSetFieldByName(Object target, String fieldName, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                    return;
                }
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException nsfe) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException("Failed to force-set field " + fieldName + " in " + target.getClass().getName(), e);
            }
        }
        throw new RuntimeException("No field named '" + fieldName + "' found in class hierarchy of " + target.getClass().getName());
    }

    @BeforeEach
    public void setUp() throws Exception {
        // --- Static mocking for Profile ---
        profileMock = mockStatic(Profile.class);
        Profile dummyProfile = mock(Profile.class);
        when(dummyProfile.isFeatureEnabled(any(Profile.Feature.class))).thenReturn(true);
        profileMock.when(Profile::getInstance).thenReturn(dummyProfile);

        // --- Stub Keycloak context and related objects ---
        when(keycloakSession.getContext()).thenReturn(keycloakContext);
        when(keycloakContext.getRealm()).thenReturn(realmModel);
        when(realmModel.getName()).thenReturn("testrealm");
        when(realmModel.getSslRequired()).thenReturn(SslRequired.NONE);
        when(keycloakContext.getUri()).thenReturn(keycloakUriInfo);
        when(keycloakUriInfo.getBaseUri()).thenReturn(baseUri);
        when(keycloakUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://example.com/{realm}"));
        MultivaluedMap<String, String> defaultQueryParams = new MultivaluedHashMap<>();
        defaultQueryParams.add("realm", "testrealm");
        defaultQueryParams.add("client_id", "dummyClientId");
        defaultQueryParams.add("redirect_uri", "dummyRedirect");
        defaultQueryParams.add("nonce", "dummyNonce");
        defaultQueryParams.add("hash", "dummyHash");
        defaultQueryParams.add("referrer", "dummyReferrer");
        when(keycloakUriInfo.getQueryParameters()).thenReturn(defaultQueryParams);

        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        when(keycloakSession.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getProviderFactoriesStream(any())).thenReturn(Stream.empty());

        cookieProvider = mock(CookieProvider.class);
        when(keycloakSession.getProvider(CookieProvider.class)).thenReturn(cookieProvider);
        when(cookieProvider.get(any(CookieType.class))).thenReturn(null);

        when(keycloakSession.getProvider(AccountProvider.class)).thenReturn(accountProvider);
        when(keycloakSession.getProvider(EventStoreProvider.class)).thenReturn(eventStoreProvider);
        when(keycloakSession.getProvider(UserProfileProvider.class)).thenReturn(userProfileProvider);
        when(keycloakSession.getProvider(UserSessionProvider.class)).thenReturn(userSessionProvider);
        when(keycloakSession.getProvider(LoginFormsProvider.class)).thenReturn(loginFormsProvider);
        when(keycloakSession.getProvider(AuthenticationSessionProvider.class)).thenReturn(authSessionProvider);

        // Stub accountProvider chain methods.
        when(accountProvider.setRealm(any(RealmModel.class))).thenReturn(accountProvider);
        when(accountProvider.setUriInfo(any(KeycloakUriInfo.class))).thenReturn(accountProvider);
        when(accountProvider.setHttpHeaders(any(HttpHeaders.class))).thenReturn(accountProvider);
        when(accountProvider.setSuccess(any())).thenReturn(accountProvider);
        when(accountProvider.setError(any(), any())).thenReturn(accountProvider);
        when(accountProvider.setError(any(), any(), any())).thenReturn(accountProvider);
        when(accountProvider.setPasswordSet(anyBoolean())).thenReturn(accountProvider);
        when(accountProvider.setProfileFormData(any())).thenReturn(accountProvider);
        when(accountProvider.createResponse(any())).thenReturn(Response.ok().build());
        when(accountProvider.setStateChecker(anyString())).thenReturn(accountProvider);
        when(accountProvider.setSessions(anyList())).thenReturn(accountProvider);
        when(accountProvider.setEvents(anyList())).thenReturn(accountProvider);
        when(accountProvider.setIdTokenHint(anyString())).thenReturn(accountProvider);
        when(accountProvider.setFeatures(anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(accountProvider);
        when(accountProvider.setAttribute(anyString(), anyString())).thenReturn(accountProvider);

        when(loginFormsProvider.setError(anyString())).thenReturn(loginFormsProvider);
        when(loginFormsProvider.createErrorPage(any(Response.Status.class))).thenReturn(Response.status(Response.Status.FORBIDDEN).build());

        when(eventBuilder.client(any(ClientModel.class))).thenReturn(eventBuilder);
        when(eventBuilder.user(any(UserModel.class))).thenReturn(eventBuilder);
        when(eventBuilder.detail(anyString(), anyString())).thenReturn(eventBuilder);
        when(eventBuilder.detail(anyString(), any(java.util.List.class))).thenReturn(eventBuilder);
        when(eventBuilder.event(any(EventType.class))).thenReturn(eventBuilder);
        when(eventBuilder.clone()).thenReturn(eventBuilder);
        doNothing().when(eventBuilder).success();

        when(keycloakContext.getConnection()).thenReturn(clientConnection);
        when(clientConnection.getRemoteAddr()).thenReturn("127.0.0.1");

        MultivaluedMap<String, String> dummyFormParams = new MultivaluedHashMap<>();
        when(dummyRequest.getDecodedFormParameters()).thenReturn(dummyFormParams);
        when(dummyRequest.getHttpMethod()).thenReturn("GET");

        ResteasyContext.clearContextData();
        ResteasyContext.pushContext(HttpHeaders.class, dummyHeaders);
        ResteasyContext.pushContext(HttpRequest.class, dummyRequest);

        MultivaluedMap<String, String> dummyRequestHeaders = new MultivaluedHashMap<>();
        dummyRequestHeaders.putSingle("Origin", "http://example.com");
        dummyRequestHeaders.putSingle("Referer", "http://example.com");
        when(dummyHeaders.getRequestHeaders()).thenReturn(dummyRequestHeaders);

        // --- Instantiate service instance ---
        testService = new TestableAccountFormService(keycloakSession, clientModel, eventBuilder);

        when(keycloakSession.getAttribute("state_checker")).thenReturn("validState");

        // Force-inject instance fields using their actual names from AccountFormService.
        forceSetFieldByName(testService, "headers", dummyHeaders);
        forceSetFieldByName(testService, "request", dummyRequest);
        forceSetFieldByName(testService, "account", accountProvider);
        forceSetFieldByName(testService, "stateChecker", "validState");
        forceSetFieldByName(testService, "eventStore", eventStoreProvider);

        dummyAuth = mock(Auth.class);
        when(dummyAuth.getUser()).thenReturn(dummyUser);
        when(dummyAuth.getClient()).thenReturn(clientModel);
        when(dummyAuth.getRealm()).thenReturn(realmModel);
        doNothing().when(dummyAuth).require(any());

        // Setup UserProvider
        when(keycloakSession.users()).thenReturn(userProvider);

        ResteasyContext.clearContextData();
    }

    @AfterEach
    public void tearDown() {
        profileMock.close();
    }

    @Test
    public void testGetResource() {
        // Execute
        Object result = testService.getResource();
        
        // Verify
        assertNotNull(result);
        assertEquals(testService, result);
    }

    @Test
    public void testClose() {
        // Execute
        testService.close();
        
        // No assertions needed as the method is empty
    }

    @Test
    public void testAccountPage_WithAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", dummyAuth);
        
        // Execute
        Response response = testService.accountPage();
        
        // Verify
        assertNotNull(response);
        verify(accountProvider).createResponse(AccountPages.ACCOUNT);
    }

    @Test
    public void testAccountPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.accountPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testPasswordPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.passwordPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFederatedIdentityPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.federatedIdentityPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testSessionsPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.sessionsPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testSessionsPage_WithAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", dummyAuth);
        
        // Mock the sessions() method to return userSessionProvider
        when(keycloakSession.sessions()).thenReturn(userSessionProvider);
        
        // Mock getUserSessionsStream to return an empty stream
        when(userSessionProvider.getUserSessionsStream(any(RealmModel.class), any(UserModel.class)))
            .thenReturn(Stream.empty());
        
        // Execute
        Response response = testService.sessionsPage();
        
        // Verify
        assertNotNull(response);
        verify(accountProvider).setSessions(anyList());
        verify(accountProvider).createResponse(AccountPages.SESSIONS);
    }

    @Test
    public void testApplicationsPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.applicationsPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testTotpPage_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.totpPage();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testProcessTotpUpdate_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.processTotpUpdate();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testProcessPasswordUpdate_WithoutAuth() {
        // Setup
        forceSetFieldByName(testService, "auth", null);
        
        // Execute
        Response response = testService.processPasswordUpdate();
        
        // Verify
        assertNotNull(response);
        // Should call login method which returns Response.ok()
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}