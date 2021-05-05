import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.special.SnapshotResult;
import com.wrapper.spotify.model_objects.specification.AudioFeatures;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.browse.GetRecommendationsRequest;
import com.wrapper.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.CreatePlaylistRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import com.wrapper.spotify.requests.data.tracks.GetAudioFeaturesForSeveralTracksRequest;
import com.wrapper.spotify.requests.data.tracks.GetAudioFeaturesForTrackRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import org.apache.hc.core5.http.ParseException;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class SpotifyInteraction {
    private static final String clientId = "d32dc8f635a74790947baf1e851edefd";
    private static final String clientSecret = "ddf79dadb5784d458934dd264be81e55";
    private static final String refreshToken = "b0KuPuLw77Z0hQhCsK-GTHoEx_kethtn357V7iqwEpCTIsLgqbBC_vQBTGC6M5rINl0FrqHK-D3cbOsMOlfyVKuQPvpyGcLcxAoLOTpYXc28nVwB7iBq2oKj9G9lHkFOUKn";
    private static URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:8000/example");
    private static String code = "AQAWiI83i4qqzffEiGo5oI4ZabpfLGRlIG2oWwRxccGNv8gYCzoW9x8XpaaSd4Vd5yZsAGj5gcuBZRhJ4dm3_zQYA7XPZOhWgNOIFXKDF-vXkmcK_2S1e2cYW3A_BU6coMWjLGGp_g7Hc6OVAOVEkgksf_e6cJdbg16D8mnaNUa7VfgqMLWQo8uZ16EOuxos2nHm6YM";
    private static String sourceId;
    private static Map<String, List<Float>> songAttributes;
    private static Map<String, List<String>> songAdjacency;
    private static Map<String, String> songUris;

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

    public static String getSourceTrack(String title, String artist) {
        try {
            searchTracksRequest = spotifyApi.searchTracks(title)
//          .market(CountryCode.SE)
//          .offset(0)
//          .includeExternal("audio")
            .build();
            final Paging<Track> trackPaging = searchTracksRequest.execute();
            for (Track track : trackPaging.getItems()) {
                if (track.getName().contentEquals(title) && track.getArtists()[0].getName().contentEquals(artist)) {
                    sourceId = track.getId();
                    return track.getId();

                }
                System.out.println(track.getName() + track.getArtists()[0].toString());
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return "";
    }

    public static void getTracks() {
        try {
            List<String> trackIds = new ArrayList<String>();
            songAttributes = new HashMap<String, List<Float>>();
            songAdjacency = new HashMap<String, List<String>>();
            songUris = new HashMap<String, String>();
            trackIds.add(sourceId);
            GetRecommendationsRequest recommendationsRequest;
            List<String> adjacencies;
            for (int i = 0; i < 31; i++) {
                adjacencies = new ArrayList<String>();
                TimeUnit.SECONDS.sleep(1);
                recommendationsRequest = spotifyApi.getRecommendations()
                        .seed_tracks(trackIds.get(i))
                        .limit(5)
                        .build();
                Recommendations recommendations = recommendationsRequest.execute();
                for (TrackSimplified track : recommendations.getTracks()) {
                    String id = track.getId();
                    adjacencies.add(id);
                    trackIds.add(id);
                }
                songAdjacency.putIfAbsent(trackIds.get(i), adjacencies);
            }
            String[] songArray1 = trackIds.subList(0,50).toArray(new String[0]);
            String[] songArray2 = trackIds.subList(50,100).toArray(new String[0]);
            String[] songArray3 = trackIds.subList(100,150).toArray(new String[0]);
            List<AudioFeatures> featuresList = new ArrayList<AudioFeatures>();
            GetAudioFeaturesForSeveralTracksRequest audioFeaturesForSeveralTracksRequest1 = spotifyApi
                    .getAudioFeaturesForSeveralTracks(songArray1)
                    .build();
            GetAudioFeaturesForSeveralTracksRequest audioFeaturesForSeveralTracksRequest2 = spotifyApi
                    .getAudioFeaturesForSeveralTracks(songArray2)
                    .build();
            GetAudioFeaturesForSeveralTracksRequest audioFeaturesForSeveralTracksRequest3 = spotifyApi
                    .getAudioFeaturesForSeveralTracks(songArray3)
                    .build();
            featuresList.addAll(List.of(audioFeaturesForSeveralTracksRequest1.execute()));
            featuresList.addAll(List.of(audioFeaturesForSeveralTracksRequest2.execute()));
            featuresList.addAll(List.of(audioFeaturesForSeveralTracksRequest3.execute()));
            for (String id : trackIds) {
                for (AudioFeatures features: featuresList) {
                    if (id.contentEquals(features.getId())) {
                        songUris.put(id, features.getUri());
                        songAttributes.putIfAbsent(id, prepareFeatures(features));
                    }
                }
            }
        } catch (IOException | SpotifyWebApiException | ParseException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static List<Float> prepareFeatures(AudioFeatures vector) {
        List<Float> transformed = new ArrayList<Float>();
        transformed.add(vector.getAcousticness());
        transformed.add(vector.getDanceability());
        transformed.add(vector.getEnergy());
        transformed.add(vector.getValence());
        Float tempoTransform = vector.getTempo() / 200;
        if (tempoTransform < 1.0) {
            transformed.add(tempoTransform);
        } else {
            transformed.add(1f);
        }
        return transformed;
    }

    public static void createPlaylist(List<String> finalIdList) {
        try {
            List<String> playlistURIs = new ArrayList<String>();
            for (String id : finalIdList) {
                playlistURIs.add(songUris.get(id));
            }
            playlistURIs.removeAll(Collections.singleton(null));
            GetCurrentUsersProfileRequest currentUsersProfileRequest = spotifyApi.getCurrentUsersProfile().build();
            String userId = currentUsersProfileRequest.execute().getId();
            CreatePlaylistRequest createPlaylistRequest = spotifyApi
                    .createPlaylist(userId, "Custom Playlist for you")
                    .collaborative(false)
                    .public_(true)
                    .build();
            String playlistId = createPlaylistRequest.execute().getId();
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                    .addItemsToPlaylist(playlistId, playlistURIs.toArray(new String[0]))
                    .build();
            SnapshotResult result = addItemsToPlaylistRequest.execute();
            System.out.println(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
        authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-private,user-read-email,playlist-modify-public,playlist-modify-private")
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
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter the Source Song");
        String title = myObj.nextLine();
        System.out.println("Enter the Artist of the Source Song");
        String artist = myObj.nextLine();
        myObj.close();
        String sourceId = getSourceTrack(title, artist);
        getTracks();
        Graph songGraph = new Graph(songAdjacency, songAttributes);
        //songGraph.printGraph();
        List<String> forPlaylist = songGraph.similarPath(sourceId);
        System.out.println(forPlaylist);
        createPlaylist(forPlaylist);
        server.stop(0);
    }


}
