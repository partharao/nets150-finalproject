import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import org.apache.hc.core5.http.ParseException;

import java.io.*;
import java.net.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.awt.Desktop;
import java.util.concurrent.TimeUnit;


public class SpotifyInteraction {
    private static final String clientId = "d32dc8f635a74790947baf1e851edefd";
    private static final String clientSecret = "ddf79dadb5784d458934dd264be81e55";
    private static final String refreshToken = "b0KuPuLw77Z0hQhCsK-GTHoEx_kethtn357V7iqwEpCTIsLgqbBC_vQBTGC6M5rINl0FrqHK-D3cbOsMOlfyVKuQPvpyGcLcxAoLOTpYXc28nVwB7iBq2oKj9G9lHkFOUKn";
    private static URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:8000/example");
    private static String code = "AQAWiI83i4qqzffEiGo5oI4ZabpfLGRlIG2oWwRxccGNv8gYCzoW9x8XpaaSd4Vd5yZsAGj5gcuBZRhJ4dm3_zQYA7XPZOhWgNOIFXKDF-vXkmcK_2S1e2cYW3A_BU6coMWjLGGp_g7Hc6OVAOVEkgksf_e6cJdbg16D8mnaNUa7VfgqMLWQo8uZ16EOuxos2nHm6YM";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private static HttpServer server;

    private static SpotifyApi spotifyApi;
    private static AuthorizationCodeUriRequest authorizationCodeUriRequest;
    private static AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest;
    private static AuthorizationCodeRequest authorizationCodeRequest;

    private static SearchTracksRequest searchTracksRequest;
    public static void authorizationCodeUri_Sync() {
        final URI uri = authorizationCodeUriRequest.execute();
        System.out.println("URI: " + uri.toString());
        SpotifyInteraction.code = uri.toString();
        HttpURLConnection con = null;
        System.out.println("You have 20 seconds to approve the app.");
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        System.out.println(query);
        if (query != null) {
            code = query.substring(5);
        }
        String response = "App authorized, thank you v much";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static void authorizationCode_Sync() {
        try {
            authorizationCodeRequest = spotifyApi.authorizationCode(code)
                    .build();
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.print("authorizecode sync: ");
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void authorizationCodeRefresh_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.print("authorizecoderefresh sync: ");
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void searchTracks_Sync() {
        try {
            searchTracksRequest = spotifyApi.searchTracks("Blinding Lights")
//          .market(CountryCode.SE)
//          .limit(10)
//          .offset(0)
//          .includeExternal("audio")
            .build();
            final Paging<Track> trackPaging = searchTracksRequest.execute();

            System.out.println("Total: " + trackPaging.getTotal());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.print("search tracks sync: ");
            System.out.println("Error: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
        authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .show_dialog(true)
                .build();
        authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                .build();

        try {
           server = HttpServer.create(new InetSocketAddress(8000), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpContext context = server.createContext("/example");
        context.setHandler(SpotifyInteraction::handleRequest);
        server.start();
        authorizationCodeUri_Sync();
        authorizationCode_Sync();
        searchTracks_Sync();
        server.stop(0);
    }


}
