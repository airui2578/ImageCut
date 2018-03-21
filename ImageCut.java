package com.td.spm.program.Utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.core.io.FileSystemResource;

import org.springframework.util.LinkedMultiValueMap;

import com.alibaba.fastjson.JSON;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.bean.ImageInfoBean;
import com.td.util.FileUtils;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import td.framework.boot.autoconfigure.rest.RealRestTemplate;
import td.framework.boot.autoconfigure.rest.RestTemplateFactory;

/**
 * @date 2012-11-26
 * @author xhw
 * 
 */
public class ImageCut {
	/**
	 * 源图片路径名称如:c:\1.jpg
	 */
	private String srcpath = "c:/test1.jpg";
	/**
	 * 剪切图片存放路径名称.如:c:\2.jpg
	 */
	private String subpath = "c:/test_end";
	/**
	 * jpg图片格式
	 */
	private static final String IMAGE_FORM_OF_JPG = "jpg";
	/**
	 * png图片格式
	 */
	private static final String IMAGE_FORM_OF_PNG = "png";
	/**
	 * 剪切点x坐标
	 */
	private int x;
	/**
	 * 剪切点y坐标
	 */
	private int y;
	/**
	 * 剪切点宽度
	 */
	private int width;
	/**
	 * 剪切点高度
	 */
	private int height;

	public String PIC_URL_UPLOAD = "http://doc.tdenergys.com/api/upload";
	public String PIC_URL_PREFIX = "http://doc.tdenergys.com/img/";

	public ImageCut() {
	}

	public ImageCut(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	// public static void main(String[] args) throws Exception {
	// ImageCut imageCut = new ImageCut(671, 203, 200, 218);
	// imageCut.cut(imageCut.getSrcpath(), imageCut.getSubpath());
	// }
	/**
	 * 返回包含所有当前已注册 ImageReader 的 Iterator，这些 ImageReader 声称能够解码指定格式。
	 * 参数：formatName - 包含非正式格式名称 .（例如 "jpeg" 或 "tiff"）等 。
	 * 
	 * @param postFix
	 *            文件的后缀名
	 * @return
	 */
	public Iterator<ImageReader> getImageReadersByFormatName(String postFix) {
		switch (postFix) {
		case IMAGE_FORM_OF_JPG:
			return ImageIO.getImageReadersByFormatName(IMAGE_FORM_OF_JPG);
		case IMAGE_FORM_OF_PNG:
			return ImageIO.getImageReadersByFormatName(IMAGE_FORM_OF_PNG);
		default:
			return ImageIO.getImageReadersByFormatName(IMAGE_FORM_OF_JPG);
		}
	}

	/**
	 * 对图片裁剪，并把裁剪完蛋新图片保存 。
	 * 
	 * @param srcpath
	 *            源图片路径
	 * @param subpath
	 *            剪切图片存放路径
	 * @throws IOException
	 */
	public String cut(InputStream is) throws IOException {
		ImageInputStream iis = null;
		try {

			// is = new FileInputStream(srcpath);
			// 获取文件的后缀名
			String postFix = ".jpeg";
			System.out.println("图片格式为：" + postFix);
			/*
			 * 返回包含所有当前已注册 ImageReader 的 Iterator，这些 ImageReader 声称能够解码指定格式。
			 * 参数：formatName - 包含非正式格式名称 .（例如 "jpeg" 或 "tiff"）等 。
			 */
			Iterator<ImageReader> it = getImageReadersByFormatName(postFix);
			ImageReader reader = it.next();
			// 获取图片流
			iis = ImageIO.createImageInputStream(is);
			/*
			 * <p>iis:读取源.true:只向前搜索 </p>.将它标记为 ‘只向前搜索'。
			 * 此设置意味着包含在输入源中的图像将只按顺序读取，可能允许 reader 避免缓存包含与以前已经读取的图像关联的数据的那些输入部分。
			 */
			reader.setInput(iis, true);
			/*
			 * <p>描述如何对流进行解码的类<p>.用于指定如何在输入时从 Java Image I/O
			 * 框架的上下文中的流转换一幅图像或一组图像。用于特定图像格式的插件 将从其 ImageReader 实现的
			 * getDefaultReadParam 方法中返回 ImageReadParam 的实例。
			 */
			ImageReadParam param = reader.getDefaultReadParam();
			/*
			 * 图片裁剪区域。Rectangle 指定了坐标空间中的一个区域，通过 Rectangle 对象
			 * 的左上顶点的坐标（x，y）、宽度和高度可以定义这个区域。
			 */
			Rectangle rect = new Rectangle(x, y, width, height);
			// 提供一个 BufferedImage，将其用作解码像素数据的目标。
			param.setSourceRegion(rect);
			/*
			 * 使用所提供的 ImageReadParam 读取通过索引 imageIndex 指定的对象，并将 它作为一个完整的
			 * BufferedImage 返回。
			 */
			BufferedImage bi = reader.read(0, param);
			ImageInfoBean imageInfoBean = null;
			byte[] bytes = imageToBytes(bi, postFix);
			File dir;
			dir = FileUtil.createTempDirectory();
			File tempFile = new File(FileNameUtil.concat(dir.getPath(), new Date().getTime() + "", true));
			FileUtils.writeByteArrayToFile(tempFile, bytes);
			String jsonString;
			try {
				jsonString = postMultiple("http://doc.tdenergys.com/api/upload", tempFile);
				String fileResponse = JSON.parseObject(jsonString, HashMap.class).get("result").toString();
				tempFile.delete();
				List<ImageInfoBean> beans = JSON.parseArray(fileResponse, ImageInfoBean.class);
				imageInfoBean = beans.get(0);
				if (imageInfoBean != null) {
					subpath = getFullPicUrl(imageInfoBean);
				}
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 保存新图片
			// ImageIO.write(bi, postFix, new File(subpath + "_" + new
			// Date().getTime() + "." + postFix));
		} finally {
			if (is != null)
				is.close();
			if (iis != null)
				iis.close();
		}
		return subpath;

	}

	public String cut(String srcpath) throws IOException {
		URL url = new URL(srcpath);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		// 设置超时间为3秒
		conn.setConnectTimeout(3 * 1000);
		// 防止屏蔽程序抓取而返回403错误
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		// 得到输入流

		InputStream is = null;
		ImageInputStream iis = null;
		try {
			// 读取图片文件
			is = conn.getInputStream();
			// is = new FileInputStream(srcpath);
			// 获取文件的后缀名
			String postFix = getPostfix(srcpath);
			System.out.println("图片格式为：" + postFix);
			/*
			 * 返回包含所有当前已注册 ImageReader 的 Iterator，这些 ImageReader 声称能够解码指定格式。
			 * 参数：formatName - 包含非正式格式名称 .（例如 "jpeg" 或 "tiff"）等 。
			 */
			Iterator<ImageReader> it = getImageReadersByFormatName(postFix);
			ImageReader reader = it.next();
			// 获取图片流
			iis = ImageIO.createImageInputStream(is);
			/*
			 * <p>iis:读取源.true:只向前搜索 </p>.将它标记为 ‘只向前搜索'。
			 * 此设置意味着包含在输入源中的图像将只按顺序读取，可能允许 reader 避免缓存包含与以前已经读取的图像关联的数据的那些输入部分。
			 */
			reader.setInput(iis, true);
			/*
			 * <p>描述如何对流进行解码的类<p>.用于指定如何在输入时从 Java Image I/O
			 * 框架的上下文中的流转换一幅图像或一组图像。用于特定图像格式的插件 将从其 ImageReader 实现的
			 * getDefaultReadParam 方法中返回 ImageReadParam 的实例。
			 */
			ImageReadParam param = reader.getDefaultReadParam();
			/*
			 * 图片裁剪区域。Rectangle 指定了坐标空间中的一个区域，通过 Rectangle 对象
			 * 的左上顶点的坐标（x，y）、宽度和高度可以定义这个区域。
			 */
			Rectangle rect = new Rectangle(x, y, width, height);
			// 提供一个 BufferedImage，将其用作解码像素数据的目标。
			param.setSourceRegion(rect);
			/*
			 * 使用所提供的 ImageReadParam 读取通过索引 imageIndex 指定的对象，并将 它作为一个完整的
			 * BufferedImage 返回。
			 */
			BufferedImage bi = reader.read(0, param);
			ImageInfoBean imageInfoBean = null;
			byte[] bytes = imageToBytes(bi, postFix);
			File dir;
			dir = FileUtil.createTempDirectory();
			File tempFile = new File(FileNameUtil.concat(dir.getPath(), new Date().getTime() + "", true));
			FileUtils.writeByteArrayToFile(tempFile, bytes);
			String jsonString;
			try {
				jsonString = postMultiple("http://doc.tdenergys.com/api/upload", tempFile);
				String fileResponse = JSON.parseObject(jsonString, HashMap.class).get("result").toString();
				tempFile.delete();
				List<ImageInfoBean> beans = JSON.parseArray(fileResponse, ImageInfoBean.class);
				imageInfoBean = beans.get(0);
				if (imageInfoBean != null) {
					subpath = getFullPicUrl(imageInfoBean);
				}
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 保存新图片
			// ImageIO.write(bi, postFix, new File(subpath + "_" + new
			// Date().getTime() + "." + postFix));
		} finally {
			if (is != null)
				is.close();
			if (iis != null)
				iis.close();
		}
		return subpath;
	}

	public String getFullPicUrl(ImageInfoBean bean) {
		try {
			return "http://doc.tdenergys.com/img/" + bean.getId() + "."
					+ bean.getFileName().split("\\.")[bean.getFileName().split("\\.").length - 1];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * http请求的一个工具类
	 *
	 * @param url
	 * @param file
	 * @return
	 * @throws ServiceException
	 */
	public static String postMultiple(String url, File file) throws ServiceException {
		String result = null;

		try {
			LinkedMultiValueMap<String, Object> request = new LinkedMultiValueMap<String, Object>();
			request.add("file[0]", new FileSystemResource(file));
			RealRestTemplate restTemplate = RestTemplateFactory.getInstance().createRealRestTemplate();
			result = (String) restTemplate.postMultipleForObject(url, request, String.class);
			return result;
		} catch (Exception var5) {
			var5.printStackTrace();
			throw new ServiceException(var5.getMessage() + url);
		}
	}

	/**
	 * 转换BufferedImage 数据为byte数组
	 * 
	 * @param image
	 *            Image对象
	 * @param format
	 *            image格式字符串.如"gif","png"
	 * @return byte数组
	 */
	public static byte[] imageToBytes(BufferedImage bImage, String format) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(bImage, format, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}

	/**
	 * 获取inputFilePath的后缀名，如："e:/test.pptx"的后缀名为："pptx"<br>
	 * 
	 * @param inputFilePath
	 * @return
	 */
	public String getPostfix(String inputFilePath) {
		return inputFilePath.substring(inputFilePath.lastIndexOf(".") + 1);
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getSrcpath() {
		return srcpath;
	}

	public void setSrcpath(String srcpath) {
		this.srcpath = srcpath;
	}

	public String getSubpath() {
		return subpath;
	}

	public void setSubpath(String subpath) {
		this.subpath = subpath;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
}
