package org.ekstep.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ilimi.common.util.ILogger;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.common.util.PlatformLogger;;
import com.ilimi.common.util.PlatformLogManager;
import com.ilimi.common.util.PlatformLogger;

/**
 * @author Rajiv Ranjan
 * 
 *         Zip source folder as story
 **/
public class ZipUtility {

	private static ILogger LOGGER = PlatformLogManager.getLogger();

	List<String> fileList;
	private String outPutZipFile = null;
	private String sourceFolder = null;

	public ZipUtility() {
		fileList = new ArrayList<String>();
	}

	public ZipUtility(String sourcePath, String zipFileName) {
		fileList = new ArrayList<String>();
		this.sourceFolder = sourcePath;
		this.outPutZipFile = zipFileName;
	}

	public ZipUtility(List<String> fileList, String outPutZipFile, String sourceFolder) {
		super();
		this.fileList = fileList;
		this.outPutZipFile = outPutZipFile;
		this.sourceFolder = sourceFolder;
	}

	public void zip() {
		ReadProperties readPro = new ReadProperties();
		try {
			outPutZipFile = readPro.getPropValues("output.zipfile");
			sourceFolder = readPro.getPropValues("source.folder");
		} catch (Exception e) {
			e.printStackTrace();
		}
		fileList = new ArrayList<String>();
		ZipUtility appZip = new ZipUtility(fileList, outPutZipFile, sourceFolder);
		appZip.generateFileList(new File(sourceFolder));
		appZip.zipIt(outPutZipFile);
	}

	/**
	 * Zip it
	 * 
	 * @param zipFile
	 *            output ZIP file location
	 */
	public void zipIt(String zipFile) {
		byte[] buffer = new byte[1024];
		try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
			LOGGER.log("Creating Zip File: " + zipFile);
			for (String file : this.fileList) {
				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);
				try (FileInputStream in = new FileInputStream(sourceFolder + File.separator + file)) {
					int len;
					while ((len = in.read(buffer)) > 0)
						zos.write(buffer, 0, len);
				}
				zos.closeEntry();
			}
		} catch (IOException ex) {
			LOGGER.log("Error! Something Went Wrong While Creating the ZIP File.", ex.getMessage(), ex);
		}
	}

	/**
	 * Traverse a directory and get all files, and add the file into fileList
	 * 
	 * @param node
	 *            file or directory
	 */
	public void generateFileList(File node) {
		// add file only
		if (node.isFile()) {
			fileList.add(generateZipEntry(node.getPath().toString()));
		}
		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename : subNote) {
				generateFileList(new File(node, filename));
			}
		}

	}

	/**
	 * Format the file path for zip
	 * 
	 * @param file
	 *            file path
	 * @return Formatted file path
	 */
	private String generateZipEntry(String file) {
		return file.substring(sourceFolder.length(), file.length());
	}
}