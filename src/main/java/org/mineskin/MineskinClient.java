package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineskinClient {

	private static final String ID_FORMAT     = "http://api.mineskin.org/get/id/%s";
	private static final String URL_FORMAT    = "http://api.mineskin.org/generate/url?url=%s&%s";
	private static final String UPLOAD_FORMAT = "http://api.mineskin.org/generate/upload?%s";
	private static final String USER_FORMAT   = "http://api.mineskin.org/generate/user/%s?%s";

	private final Executor requestExecutor;
	private final String   userAgent;

	private final JsonParser jsonParser = new JsonParser();
	private final Gson       gson       = new Gson();

	private long nextRequest = 0;

	public MineskinClient() {
		this.requestExecutor = Executors.newSingleThreadExecutor();
		this.userAgent = "MineSkin-JavaClient";
	}

	public MineskinClient(Executor requestExecutor) {
		this.requestExecutor = checkNotNull(requestExecutor);
		this.userAgent = "MineSkin-JavaClient";
	}

	public MineskinClient(Executor requestExecutor, String userAgent) {
		this.requestExecutor = checkNotNull(requestExecutor);
		this.userAgent = checkNotNull(userAgent);
	}

	public long getNextRequest() {
		return nextRequest;
	}

	/*
	 * ID
	 */

	/**
	 * Gets data for an existing Skin
	 *
	 * @param id       Skin-Id
	 * @param callback {@link SkinCallback}
	 */
	public void getSkin(int id, SkinCallback callback) {
		checkNotNull(callback);
		requestExecutor.execute(() -> {
			try {
				Connection connection = Jsoup
						.connect(String.format(ID_FORMAT, id))
						.userAgent(userAgent)
						.method(Connection.Method.GET)
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.timeout(10000);
				String body = connection.execute().body();
				handleResponse(body, callback);
			} catch (Exception e) {
				callback.exception(e);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});
	}

	/*
	 * URL
	 */

	/**
	 * Generates skin data from an URL (with default options)
	 *
	 * @param url      URL
	 * @param callback {@link SkinCallback}
	 * @see #generateUrl(String, SkinOptions, SkinCallback)
	 */
	public void generateUrl(String url, SkinCallback callback) {
		generateUrl(url, SkinOptions.none(), callback);
	}

	/**
	 * Generates skin data from an URL
	 *
	 * @param url      URL
	 * @param options  {@link SkinOptions}
	 * @param callback {@link SkinCallback}
	 */
	public void generateUrl(String url, SkinOptions options, SkinCallback callback) {
		checkNotNull(url);
		checkNotNull(options);
		checkNotNull(callback);
		requestExecutor.execute(() -> {
			try {
				if (System.currentTimeMillis() < nextRequest) {
					long delay = (nextRequest - System.currentTimeMillis());
					callback.waiting(delay);
					Thread.sleep(delay + 1000);
				}

				callback.uploading();

				Connection connection = Jsoup
						.connect(String.format(URL_FORMAT, url, options.toUrlParam()))
						.userAgent(userAgent)
						.method(Connection.Method.POST)
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.timeout(10000);
				String body = connection.execute().body();
				handleResponse(body, callback);
			} catch (Exception e) {
				callback.exception(e);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});
	}

	/*
	 * Upload
	 */

	/**
	 * Uploads and generates skin data from a local file (with default options)
	 *
	 * @param file     File to upload
	 * @param callback {@link SkinCallback}
	 */
	public void generateUpload(File file, SkinCallback callback) {
		generateUpload(file, SkinOptions.none(), callback);
	}

	/**
	 * Uploads and generates skin data from a local file
	 *
	 * @param file     File to upload
	 * @param options  {@link SkinOptions}
	 * @param callback {@link SkinCallback}
	 */
	public void generateUpload(File file, SkinOptions options, SkinCallback callback) {
		checkNotNull(file);
		checkNotNull(options);
		checkNotNull(callback);
		requestExecutor.execute(() -> {
			try {
				if (System.currentTimeMillis() < nextRequest) {
					long delay = (nextRequest - System.currentTimeMillis());
					callback.waiting(delay);
					Thread.sleep(delay + 1000);
				}

				callback.uploading();

				Connection connection = Jsoup
						.connect(String.format(UPLOAD_FORMAT, options.toUrlParam()))
						.userAgent(userAgent)
						.method(Connection.Method.POST)
						.data("file", file.getName(), new FileInputStream(file))
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.timeout(10000);
				String body = connection.execute().body();
				handleResponse(body, callback);
			} catch (Exception e) {
				callback.exception(e);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});
	}

	/*
	 * User
	 */

	/**
	 * Loads skin data from an existing player (with default options)
	 *
	 * @param uuid     {@link UUID} of the player
	 * @param callback {@link SkinCallback}
	 */
	public void generateUser(UUID uuid, SkinCallback callback) {
		generateUser(uuid, SkinOptions.none(), callback);
	}

	/**
	 * Loads skin data from an existing player
	 *
	 * @param uuid     {@link UUID} of the player
	 * @param options  {@link SkinOptions}
	 * @param callback {@link SkinCallback}
	 */
	public void generateUser(UUID uuid, SkinOptions options, SkinCallback callback) {
		checkNotNull(uuid);
		checkNotNull(options);
		checkNotNull(callback);
		requestExecutor.execute(() -> {
			try {
				if (System.currentTimeMillis() < nextRequest) {
					long delay = (nextRequest - System.currentTimeMillis());
					callback.waiting(delay);
					Thread.sleep(delay + 1000);
				}

				callback.uploading();

				Connection connection = Jsoup
						.connect(String.format(USER_FORMAT, uuid.toString(), options.toUrlParam()))
						.userAgent(userAgent)
						.method(Connection.Method.GET)
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.timeout(10000);
				String body = connection.execute().body();
				handleResponse(body, callback);
			} catch (Exception e) {
				callback.exception(e);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});
	}

	void handleResponse(String body, SkinCallback callback) {
		try {
			JsonObject jsonObject = jsonParser.parse(body).getAsJsonObject();
			if (jsonObject.has("error")) {
				callback.error(jsonObject.get("error").getAsString());
				return;
			}

			Skin skin = gson.fromJson(jsonObject, Skin.class);
			this.nextRequest = System.currentTimeMillis() + ((long) (skin.nextRequest * 1000L));
			callback.done(skin);
		} catch (JsonParseException e) {
			callback.parseException(e, body);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

}
