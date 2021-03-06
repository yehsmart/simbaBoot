package com.simba.rest.asr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.impl.util.Base64;

import com.simba.rest.asr.model.HttpResponse;

public class HttpUtil {

	/*
	 * 计算MD5+BASE64
	 */
	public static String MD5Base64(byte[] s) throws UnsupportedEncodingException {
		if (s == null) {
			return null;
		}
		String encodeStr = "";
		// string 编码必须为utf-8
		MessageDigest mdTemp;
		try {
			mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(s);
			byte[] md5Bytes = mdTemp.digest();
			encodeStr = new String(Base64.encode(md5Bytes));
			/*
			 * java 1.8以上版本支持 Encoder encoder = Base64.getEncoder(); encodeStr =
			 * encoder.encodeToString(md5Bytes);
			 */
		} catch (Exception e) {
			throw new Error("Failed to generate MD5 : " + e.getMessage());
		}
		return encodeStr;
	}

	/*
	 * 计算 HMAC-SHA1
	 */
	public static String HMACSha1(String data, String key) {
		String result;
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			byte[] rawHmac = mac.doFinal(data.getBytes());
			result = new String(Base64.encode(rawHmac));
			/*
			 * java 1.8以上版本支持 Encoder encoder = Base64.getEncoder(); result =
			 * encoder.encodeToString(rawHmac);
			 */
		} catch (Exception e) {
			throw new Error("Failed to generate HMAC : " + e.getMessage());
		}
		return result;
	}

	/*
	 * 等同于javaScript中的 new Date().toUTCString();
	 */
	public static String toGMTString(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
		df.setTimeZone(new java.util.SimpleTimeZone(0, "GMT"));
		return df.format(date);
	}

	/*
	 * 发送POST请求
	 */
	public static HttpResponse sendAsrPost(byte[] audioData, String audioFormat, int sampleRate, String url, String ak_id, String ak_secret) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		HttpResponse response = new HttpResponse();
		try {
			URL realUrl = new URL(url);
			/*
			 * http header 参数
			 */
			String method = "POST";
			String accept = "application/json";
			String content_type = "audio/" + audioFormat + ";samplerate=" + sampleRate;
			int length = audioData.length;
			String date = toGMTString(new Date());
			// 1.对body做MD5+BASE64加密
			String bodyMd5 = MD5Base64(audioData);
			String md52 = MD5Base64(bodyMd5.getBytes());
			String stringToSign = method + "\n" + accept + "\n" + md52 + "\n" + content_type + "\n" + date;
			// 2.计算 HMAC-SHA1
			String signature = HMACSha1(stringToSign, ak_secret);
			// 3.得到 authorization header
			String authHeader = "Dataplus " + ak_id + ":" + signature;
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", accept);
			conn.setRequestProperty("content-type", content_type);
			conn.setRequestProperty("date", date);
			conn.setRequestProperty("Authorization", authHeader);
			conn.setRequestProperty("Content-Length", String.valueOf(length));
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			OutputStream stream = conn.getOutputStream();
			// 发送请求参数
			stream.write(audioData);
			// flush输出流的缓冲
			stream.flush();
			stream.close();
			response.setStatus(conn.getResponseCode());
			// 定义BufferedReader输入流来读取URL的响应
			if (response.getStatus() == 200) {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			} else {
				in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			}
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
			if (response.getStatus() == 200) {
				response.setResult(result);
				response.setMessage("OK");
			} else {
				response.setMessage(result);
			}
			System.out.println("post response status code: [" + response.getStatus() + "], response massage : [" + response.getMessage() + "] ,result :[" + response.getResult() + "]");
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
		}
		return response;
	}

	/*
	 * 发送POST请求
	 */
	public static HttpResponse sendTtsPost(String textData, String audioType, String audioName, String url, String ak_id, String ak_secret) {
		PrintWriter out = null;
		BufferedReader in = null;
		FileOutputStream fileOutputStream = null;
		String result = "";
		HttpResponse response = new HttpResponse();
		try {
			URL realUrl = new URL(url);
			/*
			 * http header 参数
			 */
			String method = "POST";
			String content_type = "text/plain";
			String accept = "audio/" + audioType + ",application/json";
			int length = textData.length();
			String date = toGMTString(new Date());
			// 1.对body做MD5+BASE64加密
			String bodyMd5 = MD5Base64(textData.getBytes());
			// String md52 = MD5Base64(bodyMd5.getBytes());
			String stringToSign = method + "\n" + accept + "\n" + bodyMd5 + "\n" + content_type + "\n" + date;
			// 2.计算 HMAC-SHA1
			String signature = HMACSha1(stringToSign, ak_secret);
			// 3.得到 authorization header
			String authHeader = "Dataplus " + ak_id + ":" + signature;
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", accept);
			conn.setRequestProperty("content-type", content_type);
			conn.setRequestProperty("date", date);
			conn.setRequestProperty("Authorization", authHeader);
			conn.setRequestProperty("Content-Length", String.valueOf(length));
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			OutputStream stream = conn.getOutputStream();
			// 发送请求参数
			stream.write(textData.getBytes());
			// flush输出流的缓冲
			stream.flush();
			stream.close();
			response.setStatus(conn.getResponseCode());
			// 定义BufferedReader输入流来读取URL的响应
			InputStream is = null;
			String line = null;
			if (response.getStatus() == 200) {
				is = conn.getInputStream();
			} else {
				in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
				while ((line = in.readLine()) != null) {
					result += line;
				}
			}
			File ttsFile = new File(audioName + "." + audioType);
			fileOutputStream = new FileOutputStream(ttsFile);
			byte[] b = new byte[1024];
			int len = 0;
			while (is != null && (len = is.read(b)) != -1) { // 先读到内存
				fileOutputStream.write(b, 0, len);
			}
			if (response.getStatus() == 200) {
				response.setResult(result);
				response.setMessage("OK");
				System.out.println("post response status code: [" + response.getStatus() + "], generate tts audio file :" + audioName + "." + audioType);
			} else {
				response.setMessage(result);
				System.out.println("post response status code: [" + response.getStatus() + "], response massage : [" + response.getMessage() + "]");
			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(fileOutputStream);
		}
		return response;
	}
}
