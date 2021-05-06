import com.neovisionaries.i18n.CountryCode;
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
    private static URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:8000/example");
    private static String code = "";
    private static String sourceId;
    private static Map<String, List<Float>> songAttributes;
    private static Map<String, String> songUris;
    private static HttpServer server;
    private static SpotifyApi spotifyApi;
    private static AuthorizationCodeUriRequest authorizationCodeUriRequest;
    private static AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest;
    private static AuthorizationCodeRequest authorizationCodeRequest;

    private static SearchTracksRequest searchTracksRequest;
    /*
    Generates an authorization URL to allow the app to access a user's playlists.
     */
    public static void authorizationCodeUri_Sync() {
        final URI uri = authorizationCodeUriRequest.execute();
        System.out.println("URI: " + uri.toString());
        SpotifyInteraction.code = uri.toString();
        System.out.println("You have 20 seconds to approve the app.");
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    Handles the incoming HTTP request and parses out the authorization code for the API
     */
    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        System.out.println(query);
        if (query != null) {
            code = query.substring(5);
        }
        String response = "App authorized, thank you for using our playlist creator.";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /*
    Gets an access token using the authorization code.
     */
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

    /*
    Would refresh the access token for continued use of the API if runtime was longer than 6 minutes.
     */
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

    /*
    Gets the source song by searching for the song within the US market, then checking the results for exact matches of
    both track name and artist name.
     */
    public static String getSourceTrack(String title, String artist) {
        System.out.println("Getting your source track, " + title + " by " + artist);
        try {
            searchTracksRequest = spotifyApi.searchTracks(title)
                .market(CountryCode.US)
                .build();
            final Paging<Track> trackPaging = searchTracksRequest.execute();
            if (trackPaging == null) {
                System.out.println("Could not find source track: request did not execute");
                return "";
            }
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
        System.out.println("Could not find source track: string did not match any results");
        return "";
    }

    /*
    Gets 150 tracks to put in the graph by getting 5 recommended tracks for the source, then 5 recommended tracks for
    each of the recommendations and so on. Adds the track ids to a list, and a map from id to uri for each song. It also
    gets the audio features for each track, gets the important ones, standardizes them in a vector then adds them to a
    map.
     */
    public static void getTracks() {
        System.out.println("Gathering a selection of tracks to create graph.");
        try {
            List<String> trackIds = new ArrayList<String>();
            songAttributes = new HashMap<String, List<Float>>();
            songUris = new HashMap<String, String>();
            trackIds.add(sourceId);
            GetRecommendationsRequest recommendationsRequest;
            for (int i = 0; i < 31; i++) {
                TimeUnit.SECONDS.sleep(1);
                recommendationsRequest = spotifyApi.getRecommendations()
                        .seed_tracks(trackIds.get(i))
                        .market(CountryCode.US)
                        .limit(5)
                        .build();
                Recommendations recommendations = recommendationsRequest.execute();
                for (TrackSimplified track : recommendations.getTracks()) {
                    String id = track.getId();
                    trackIds.add(id);
                }
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

    /*
    Standardizes the vector of audio features for a particular song.
     */
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

    /*
    Creates the playlist by first getting User ID, creating the playlist with the specified name, the adding all the
    songs using their URIS.
     */
    public static void createPlaylist(List<String> finalIdList, String name) {
        if (name == null || name.contentEquals("")) {
            System.out.println("You must specify a name");
            return;
        }
        try {
            List<String> playlistURIs = new ArrayList<String>();
            for (String id : finalIdList) {
                playlistURIs.add(songUris.get(id));
            }
            playlistURIs.removeAll(Collections.singleton(null));
            GetCurrentUsersProfileRequest currentUsersProfileRequest = spotifyApi.getCurrentUsersProfile().build();
            String userId = currentUsersProfileRequest.execute().getId();
            CreatePlaylistRequest createPlaylistRequest = spotifyApi
                    .createPlaylist(userId, name)
                    .collaborative(false)
                    .public_(true)
                    .build();
            String playlistId = createPlaylistRequest.execute().getId();
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                    .addItemsToPlaylist(playlistId, playlistURIs.toArray(new String[0]))
                    .build();
            SnapshotResult result = addItemsToPlaylistRequest.execute();
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
        System.out.println("Enter the desired name for the playlist.");
        String playlistName = myObj.nextLine();
        myObj.close();
        String sourceId = getSourceTrack(title, artist);
        getTracks();
        System.out.println("Creating graph.");
        Graph songGraph = new Graph(songAttributes);
        System.out.println("Finding path.");
        List<String> forPlaylist = songGraph.similarPath(sourceId);
        createPlaylist(forPlaylist, playlistName);
        System.out.println("Playlist " + playlistName +" created. Check it out on Spotify!");
        server.stop(0);
    }


}
