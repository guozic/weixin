package com.guozi.wxmp.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @ClassName: HttpUtil
 * @Description: 
 * 				Http请求工具类
 * @author WUGUO
 * @date 2016年4月5日 下午3:41:43
 *
 */
public abstract class HttpUtil {

	/**
	 * 
	 * @Title: sendGet
	 * @Description: 
	 * 				发送get请求
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:00:18
	 * @param @return    设定文件
	 * @return String    请求成功返回数据，请求失败返回error
	 * @throws TimeoutException
	 */
	public abstract String sendGet() throws TimeoutException;
	
	/**
	 * 
	 * @Title: sendPost
	 * @Description: 
	 * 				发送post请求，如果postData中包含File类型则content-ty：multipart/form-data否则采用默认类型application/x-www-form-urlencoded
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:03:07
	 * @param @param postData
	 * @param @return    设定文件
	 * @return String    返回类型
	 * @throws TimeoutException
	 */
	public abstract String sendPost(Map<String, Object> postData)  throws TimeoutException, UnsupportedEncodingException;
	
	/**
	 * 
	 * @Title: sendPost
	 * @Description: 
	 * 				发送post请求，不带参，请求数据直接写入数据流中
	 * 				需要指定类型，调用此方法一般指定类型为text/xml或者application/json
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:16:39
	 * @param @param body
	 * @param @param contentType
	 * @param @return    设定文件
	 * @return String    返回类型
	 * @throws	TimeoutException
	 */
	public abstract String sendPost(String body, String contentType) throws TimeoutException;
	
	/**
	 * 
	 * @Title: setTimeout
	 * @Description: 
	 * 				设置请求超时时间，单位毫秒，设置后请求和响应时间同步设置
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:36:43
	 * @param @param timeout    设定文件
	 * @return void    返回类型
	 * @throws
	 */
	public abstract void setTimeout(int timeout);
	
	/**
	 * 
	 * @Title: setEncoding
	 * @Description: 
	 * 				设置编码方式，设置后请求和响应同步设置，默认采用utf-8编码
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:40:00
	 * @param @param encoding    设定文件
	 * @return void    返回类型
	 * @throws
	 */
	public abstract void setEncoding(String encoding);
	
	/**
	 * 
	 * @Title: setRequestProperty
	 * @Description: 设置头信息
	 * @author WUGUO
	 * @date 2016年11月22日 上午3:02:30
	 * @param name
	 * @param value
	 * @return void
	 */
	public abstract void setRequestProperty(String name, String value);
	
	/**
	 * 返回一个实现类实例
	 * @Title: getInstance
	 * @Description: 
	 * 			获取一个操作实例
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:24:05
	 * @param @param url
	 * @param @return    设定文件
	 * @return HttpUtil    返回类型
	 * @throws
	 */
	public static HttpUtil getInstance(String url) throws MalformedURLException{
		if(url.toLowerCase().indexOf("https") != -1){
			return new HttpsImpl(url);
		}
		return new HttpImpl(url);
	}
	
}

class HttpImpl extends HttpUtil{
	
	URL url;															//请求地址
	HttpURLConnection connection;								//jdkhttp工具类
	
	String boundary = "--------boundary-------";  						//分割符
	int timeout = 10*1000;												//超时时间默认10秒
	String encoding = "utf-8";											//默认编码为utf-8
	Map<String, String> textParams = new HashMap<String, String>();  	//文本参数
    Map<String, File> fileParams = new HashMap<String, File>();  		//文件参数
    
    Map<String, String> headers = new HashMap<String, String>();		//添加头信息
    
    BufferedReader input = null;										//输入流，读取响应结果
    DataOutputStream output = null;										//输出流，向请求中写入数据
	
	/**
	 * 
	 * <p>Title: HttpUtilImpl实例化</p>
	 * <p>Description: 实例化需要参数url，如果url有问题，抛出异常</p>
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:31:09
	 * @param url
	 * @throws MalformedURLException
	 */
	public HttpImpl(String url) throws MalformedURLException{
		this.url  = new URL(url);
	}
	
	/**
	 * 
	 * @Title: initConnection
	 * @Description: 	
	 * 				初始化连接与参数设置
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:59:46
	 * @param  requestMethod   请求方式，支持“POST”，“GET”
	 * @return void    返回类型
	 * @throws IOException
	 */
	private void initConnection(String requestMethod) throws IOException{
		this.connection = (HttpURLConnection) this.url.openConnection();  
		this.connection.setUseCaches(false); 
		
		//post请求需要设置dooutput,doinput
		if("POST".equals(requestMethod)){
			this.connection.setDoOutput(true); 					
			this.connection.setDoInput(true); 					
		}
		this.connection.setConnectTimeout(timeout);
		this.connection.setReadTimeout(timeout);
		
		//设置头信息
		for(String name : this.headers.keySet()){
			connection.setRequestProperty(name, this.headers.get(name));
		}
		
		this.connection.setRequestMethod(requestMethod); 
	}
	
	/**
	 * 
	 * @Title: readResponse
	 * @Description: 
	 * 				读取请求返回接口，如果请求失败返回error
	 * @author WUGUO
	 * @date 2016年4月5日 下午5:40:26
	 * @param @return
	 * @param @throws IOException    设定文件
	 * @return String    请求失败返回error
	 * @throws IOException
	 */
	private String readResponse(){
		String result = "";
		try{
			int code = this.connection.getResponseCode();
			
			/*
			 * 状态码200为正常
			 * 其他参考http://tool.oschina.net/commons?type=5
			 */
			if (code == 200) { // 若响应码以2开头则读取响应头总的返回信息

				input = new BufferedReader(new InputStreamReader(this.connection.getInputStream(), encoding));

				char[] charBuffer = new char[1024];
				StringBuffer sb = new StringBuffer();
				int length = -1;
				while ((length = input.read(charBuffer)) != -1) {
					sb.append(charBuffer.length == length ? charBuffer : Arrays.copyOf(charBuffer, length));
				}

				result = sb.toString();
			} else { 	// 若响应码不以2开头则返回错误信息.
				result = "error";
			}
		}catch(IOException ex){
			ex.printStackTrace();
			result = "error";
		}finally{
			closeConnection();
		}
		
		return result;
	}
	
	/**
	 * 
	 * @Title: closeConnection
	 * @Description: 
	 * 				关闭连接，释放资源
	 * @author WUGUO
	 * @date 2016年4月5日 下午5:45:18
	 * @param     设定文件
	 * @return void    返回类型
	 * @throws
	 */
	private void closeConnection(){
		try{
			if (input != null) {
				input.close();
			}
		}catch(IOException ex){
			input = null;
		}
		try{
			if (output != null) {
				output.close();
			}
		}catch(IOException ex){
			output = null;
		}
		if (connection != null) {
			connection.disconnect();
		}
	}
	
	/**
	 * 
	 * @Title: getContentType
	 * @Description: 获取文件的上传类型，图片格式为image/png,image/jpg等。非图片为application/octet-stream  
	 * @author WUGUO
	 * @date 2016年4月7日 下午3:08:14
	 * @param @param f
	 * @param @return
	 * @param @throws Exception    设定文件
	 * @return String    返回类型
	 * @throws
	 */
    private String getContentType(File file){  
        ImageInputStream imagein = null;
		try {
			imagein = ImageIO.createImageInputStream(file);
		} catch (IOException e) {
			//e.printStackTrace();
		}  
		
        if (imagein == null) {  
            return "application/octet-stream";  
        }  
        
        Iterator<ImageReader> it = ImageIO.getImageReaders(imagein);  
        
        try {
			imagein.close();
		} catch (IOException e) {
			//e.printStackTrace();
		} 
        
		if (!it.hasNext()) {
			return "application/octet-stream";
		}
         
         
        try {
			return "image/" + it.next().getFormatName().toLowerCase(); //将FormatName返回的值转换成小写，默认为大写  
		} catch (IOException e) {
			//e.printStackTrace();
		}
        return "application/octet-stream";
  
    } 
    
    /**
     * 
     * @Title: getBytes
     * @Description: 把文件转换成字节数组  
     * @author WUGUO
     * @date 2016年4月7日 下午4:44:23
     * @param @param f
     * @param @return
     * @param @throws Exception    设定文件
     * @return byte[]    返回类型
     * @throws IOException 
     */
    private byte[] getBytes(File file) throws IOException{  
        FileInputStream in = new FileInputStream(file);  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        byte[] b = new byte[1024];  
        int n;  
        while ((n = in.read(b)) != -1) {  
            out.write(b, 0, n);  
        }  
        in.close();  
        return out.toByteArray();  
    } 

	@Override
	public String sendGet() throws TimeoutException{
		String result = "error";
		//初始化设置
		try {
			initConnection("GET");
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		}
		
		try {
			this.connection.connect();
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}

	/**
	 * 
	 * @Title: sendPost
	 * @Description: 
	 * 				发送post请求，如果postData中包含File类型则content-type：multipart/form-data否则采用默认类型application/x-www-form-urlencoded
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:03:07
	 * @param @param postData
	 * @param @return    设定文件
	 * @return String    返回类型
	 * @throws TimeoutException
	 * @throws UnsupportedEncodingException 
	 */
	@Override
	public String sendPost(Map<String, Object> postData) throws TimeoutException, UnsupportedEncodingException {
		String result = "error";
		
		
		/*
		 * 判断是content-type
		 * 	如果为：multipart/form-data，直接拼接&分割对象数据
		 *  如果为：application/x-www-form-urlencoded，按照文本域方式请求数据。
		 *  如果fileparams有值则判定为application/x-www-form-urlencoded
		 */
		for(String key : postData.keySet()){
			if(postData.get(key) instanceof File){		//只做文件判断，其他都做字符处理
				fileParams.put(key, (File)postData.get(key));
			}else{
				textParams.put(key, postData.get(key).toString());
			}
		}
		
		// 初始化设置
		try {
			initConnection("POST");
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		}

		//如果为空content-type=multipart/form-data
		if(!fileParams.isEmpty()){
			this.connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); 
		}
		
		
		try {
			this.connection.connect();
			this.output = new DataOutputStream(this.connection.getOutputStream());

			// 开始写入说数据
			if (!fileParams.isEmpty()) {
				// 写入文件

				Set<String> fileKeySets = fileParams.keySet();
				for (String fileKeySet : fileKeySets) {
					File file = fileParams.get(fileKeySet);

					this.output.writeBytes("--" + this.boundary + "\r\n");
					this.output
							.writeBytes("Content-Disposition: form-data; name=\""
									+ fileKeySet
									+ "\"; filename=\""
									+ URLEncoder.encode(file.getName(),
											encoding) + "\"\r\n");
					this.output.writeBytes("Content-Type: "
							+ getContentType(file) + "\r\n");
					this.output.writeBytes("\r\n");
					this.output.write(getBytes(file));
					this.output.writeBytes("\r\n");

				}

				// 写入字符串
				Set<String> stringKeySets = textParams.keySet();
				for (String stringKeySet : stringKeySets) {
					String value = textParams.get(stringKeySet);
					this.output.writeBytes("--" + boundary + "\r\n");
					this.output
							.writeBytes("Content-Disposition: form-data; name=\""
									+ stringKeySet + "\"\r\n");
					this.output.writeBytes("\r\n");
					this.output.writeBytes(URLEncoder.encode(value, encoding)
							+ "\r\n");
				}// 添加结尾
				this.output.writeBytes("--" + boundary + "--" + "\r\n");
				this.output.writeBytes("\r\n");

			} else {
				String paramStr = "";
				Set<String> stringKeySets = textParams.keySet();
				// 取出所有参数进行构造
				for (String stringKeySet : stringKeySets) {
					paramStr += stringKeySet
							+ "="
							+ URLEncoder.encode(textParams.get(stringKeySet),
									encoding) + "&";
				}
				paramStr = paramStr.length() > 0 ? paramStr.substring(0,paramStr.length() - 1) : paramStr;
				this.output.writeBytes(paramStr);
			}
			
			this.output.flush();
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}

	@Override
	public String sendPost(String body, String contentType) throws TimeoutException {
		String result = "error";
		//初始化设置
		try {
			initConnection("POST");
			this.connection.setRequestProperty("Content-Type", contentType);
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		}
		
		try {
			this.connection.connect();
			
			this.output = new DataOutputStream(this.connection.getOutputStream());
			
			this.output.write(body.getBytes(encoding));
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}
	
	/**
	 * 超时时间默认10秒
	 * 单位为毫秒
	 */
	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * 默认编码为utf-8
	 */
	@Override
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setRequestProperty(String name, String value){
		this.headers.put(name, value);
	}

	
}

class HttpsImpl extends HttpUtil{
	
	URL url;															//请求地址
	HttpsURLConnection connection;								//jdkhttp工具类
	
	String boundary = "--------boundary-------";  						//分割符
	int timeout = 10*1000;												//超时时间默认10秒
	String encoding = "utf-8";											//默认编码为utf-8
	Map<String, String> textParams = new HashMap<String, String>();  	//文本参数
    Map<String, File> fileParams = new HashMap<String, File>();  		//文件参数
    
    Map<String, String> headers = new HashMap<String, String>();		//添加头信息
    
    BufferedReader input = null;										//输入流，读取响应结果
    DataOutputStream output = null;										//输出流，向请求中写入数据
	
	/**
	 * 
	 * <p>Title: HttpUtilImpl实例化</p>
	 * <p>Description: 实例化需要参数url，如果url有问题，抛出异常</p>
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:31:09
	 * @param url
	 * @throws MalformedURLException
	 */
	public HttpsImpl(String url) throws MalformedURLException{
		this.url  = new URL(url);
	}
	
	/**
	 * 
	 * @Title: initConnection
	 * @Description: 	
	 * 				初始化连接与参数设置
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:59:46
	 * @param  requestMethod   请求方式，支持“POST”，“GET”
	 * @return void    返回类型
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 */
	private void initConnection(String requestMethod) throws IOException, NoSuchAlgorithmException, KeyManagementException{
		
		this.connection = (HttpsURLConnection) this.url.openConnection(); 
		
		//绕过证书
		SSLContext ctx = SSLContext.getInstance("TLS");  
        ctx.init(null, new TrustManager[] {new DefaultTrustManager()}, null);  
        this.connection.setSSLSocketFactory(ctx.getSocketFactory());
        this.connection.setHostnameVerifier(new THostnameVerifier());
		
		
		this.connection.setUseCaches(false); 
		
		//post请求需要设置dooutput,doinput
		if("POST".equals(requestMethod)){
			this.connection.setDoOutput(true); 					
			this.connection.setDoInput(true); 					
		}
		this.connection.setConnectTimeout(timeout);
		this.connection.setReadTimeout(timeout);
		
		//设置头信息
		for(String name : this.headers.keySet()){
			connection.setRequestProperty(name, this.headers.get(name));
		}
		
		this.connection.setRequestMethod(requestMethod); 
	}
	
	/**
	 * 
	 * @Title: readResponse
	 * @Description: 
	 * 				读取请求返回接口，如果请求失败返回error
	 * @author WUGUO
	 * @date 2016年4月5日 下午5:40:26
	 * @param @return
	 * @param @throws IOException    设定文件
	 * @return String    请求失败返回error
	 * @throws IOException
	 */
	private String readResponse(){
		String result = "";
		try{
			int code = this.connection.getResponseCode();
			
			/*
			 * 状态码200为正常
			 * 其他参考http://tool.oschina.net/commons?type=5
			 */
			if (code == 200) { // 若响应码以2开头则读取响应头总的返回信息

				input = new BufferedReader(new InputStreamReader(this.connection.getInputStream(), encoding));

				char[] charBuffer = new char[1024];
				StringBuffer sb = new StringBuffer();
				int length = -1;
				while ((length = input.read(charBuffer)) != -1) {
					sb.append(charBuffer.length == length ? charBuffer : Arrays.copyOf(charBuffer, length));
				}

				result = sb.toString();
			} else { 	// 若响应码不以2开头则返回错误信息.
				result = "error";
			}
		}catch(IOException ex){
			ex.printStackTrace();
			result = "error";
		}finally{
			closeConnection();
		}
		
		return result;
	}
	
	/**
	 * 
	 * @Title: closeConnection
	 * @Description: 
	 * 				关闭连接，释放资源
	 * @author WUGUO
	 * @date 2016年4月5日 下午5:45:18
	 * @param     设定文件
	 * @return void    返回类型
	 * @throws
	 */
	private void closeConnection(){
		try{
			if (input != null) {
				input.close();
			}
		}catch(IOException ex){
			input = null;
		}
		try{
			if (output != null) {
				output.close();
			}
		}catch(IOException ex){
			output = null;
		}
		if (connection != null) {
			connection.disconnect();
		}
	}
	
	/**
	 * 
	 * @Title: getContentType
	 * @Description: 获取文件的上传类型，图片格式为image/png,image/jpg等。非图片为application/octet-stream  
	 * @author WUGUO
	 * @date 2016年4月7日 下午3:08:14
	 * @param @param f
	 * @param @return
	 * @param @throws Exception    设定文件
	 * @return String    返回类型
	 * @throws
	 */
    private String getContentType(File file){  
        ImageInputStream imagein = null;
		try {
			imagein = ImageIO.createImageInputStream(file);
		} catch (IOException e) {
			//e.printStackTrace();
		}  
		
        if (imagein == null) {  
            return "application/octet-stream";  
        }  
        
        Iterator<ImageReader> it = ImageIO.getImageReaders(imagein);  
        
        try {
			imagein.close();
		} catch (IOException e) {
			//e.printStackTrace();
		} 
        
		if (!it.hasNext()) {
			return "application/octet-stream";
		}
         
         
        try {
			return "image/" + it.next().getFormatName().toLowerCase(); //将FormatName返回的值转换成小写，默认为大写  
		} catch (IOException e) {
			//e.printStackTrace();
		}
        return "application/octet-stream";
  
    } 
    
    /**
     * 
     * @Title: getBytes
     * @Description: 把文件转换成字节数组  
     * @author WUGUO
     * @date 2016年4月7日 下午4:44:23
     * @param @param f
     * @param @return
     * @param @throws Exception    设定文件
     * @return byte[]    返回类型
     * @throws IOException 
     */
    private byte[] getBytes(File file) throws IOException{  
        FileInputStream in = new FileInputStream(file);  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        byte[] b = new byte[1024];  
        int n;  
        while ((n = in.read(b)) != -1) {  
            out.write(b, 0, n);  
        }  
        in.close();  
        return out.toByteArray();  
    } 

	@Override
	public String sendGet() throws TimeoutException{
		String result = "error";
		//初始化设置
		try {
			initConnection("GET");
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		} catch (KeyManagementException ex) {
			ex.printStackTrace();
			return result;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return result;
		}
		
		try {
			this.connection.connect();
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}

	/**
	 * 
	 * @Title: sendPost
	 * @Description: 
	 * 				发送post请求，如果postData中包含File类型则content-type：multipart/form-data否则采用默认类型application/x-www-form-urlencoded
	 * @author WUGUO
	 * @date 2016年4月5日 下午4:03:07
	 * @param @param postData
	 * @param @return    设定文件
	 * @return String    返回类型
	 * @throws TimeoutException
	 * @throws UnsupportedEncodingException 
	 */
	@Override
	public String sendPost(Map<String, Object> postData) throws TimeoutException, UnsupportedEncodingException {
		String result = "error";
		
		
		/*
		 * 判断是content-type
		 * 	如果为：multipart/form-data，直接拼接&分割对象数据
		 *  如果为：application/x-www-form-urlencoded，按照文本域方式请求数据。
		 *  如果fileparams有值则判定为application/x-www-form-urlencoded
		 */
		for(String key : postData.keySet()){
			if(postData.get(key) instanceof File){		//只做文件判断，其他都做字符处理
				fileParams.put(key, (File)postData.get(key));
			}else{
				textParams.put(key, postData.get(key).toString());
			}
		}
		
		// 初始化设置
		try {
			initConnection("POST");
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		} catch (KeyManagementException ex) {
			ex.printStackTrace();
			return result;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return result;
		}

		//如果为空content-type=multipart/form-data
		if(!fileParams.isEmpty()){
			this.connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); 
		}
		
		
		try {
			this.connection.connect();
			this.output = new DataOutputStream(this.connection.getOutputStream());

			// 开始写入说数据
			if (!fileParams.isEmpty()) {
				// 写入文件

				Set<String> fileKeySets = fileParams.keySet();
				for (String fileKeySet : fileKeySets) {
					File file = fileParams.get(fileKeySet);

					this.output.writeBytes("--" + this.boundary + "\r\n");
					this.output
							.writeBytes("Content-Disposition: form-data; name=\""
									+ fileKeySet
									+ "\"; filename=\""
									+ URLEncoder.encode(file.getName(),
											encoding) + "\"\r\n");
					this.output.writeBytes("Content-Type: "
							+ getContentType(file) + "\r\n");
					this.output.writeBytes("\r\n");
					this.output.write(getBytes(file));
					this.output.writeBytes("\r\n");

				}

				// 写入字符串
				Set<String> stringKeySets = textParams.keySet();
				for (String stringKeySet : stringKeySets) {
					String value = textParams.get(stringKeySet);
					this.output.writeBytes("--" + boundary + "\r\n");
					this.output.writeBytes("Content-Disposition: form-data; name=\"" + stringKeySet + "\"\r\n");
					this.output.writeBytes("\r\n");
					this.output.writeBytes(URLEncoder.encode(value, encoding) + "\r\n");
				}// 添加结尾
				this.output.writeBytes("--" + boundary + "--" + "\r\n");
				this.output.writeBytes("\r\n");

			} else {
				String paramStr = "";
				Set<String> stringKeySets = textParams.keySet();
				// 取出所有参数进行构造
				for (String stringKeySet : stringKeySets) {
					paramStr += stringKeySet + "=" + URLEncoder.encode(textParams.get(stringKeySet), encoding) + "&";
				}
				paramStr = paramStr.length() > 0 ? paramStr.substring(0,paramStr.length() - 1) : paramStr;
				this.output.writeBytes(paramStr);
			}
			
			this.output.flush();
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}

	@Override
	public String sendPost(String body, String contentType) throws TimeoutException {
		String result = "error";
		//初始化设置
		try {
			initConnection("POST");
			this.connection.setRequestProperty("Content-Type", contentType);
		} catch (IOException ex) {
			ex.printStackTrace();
			return result;
		} catch (KeyManagementException ex) {
			ex.printStackTrace();
			return result;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return result;
		}
		
		try {
			this.connection.connect();
			
			this.output = new DataOutputStream(this.connection.getOutputStream());
			
			this.output.write(body.getBytes(encoding));
		} catch (SocketTimeoutException ex) {
			throw new TimeoutException(ex.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		
		result = readResponse();
		
		return result;
	}
	
	/**
	 * 超时时间默认10秒
	 * 单位为毫秒
	 */
	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * 默认编码为utf-8
	 */
	@Override
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setRequestProperty(String name, String value){
		this.headers.put(name, value);
	}

	
}

/**
 * 
 * @ClassName: DefaultTrustManager
 * @Description: 普通验证（忽略验证）
 * @author one
 * @date 2018年5月19日 上午11:49:39
 *
 */
class DefaultTrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
			String authType) throws CertificateException {
		
	}

	@Override
	public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
			String authType) throws CertificateException {
		
	}

	@Override
	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		return null;
	}  
	  
}

/**
 * 
 * @ClassName: THostnameVerifier
 * @Description: 允许所有主机访问
 * @author one
 * @date 2018年5月19日 上午11:53:22
 *
 */
class THostnameVerifier implements HostnameVerifier {
	@Override
	public boolean verify(String s, SSLSession sslSession) {
		// 解决由于服务器证书问题导致HTTPS无法访问的情况
		return true;
	}
}