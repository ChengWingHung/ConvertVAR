import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;

import common.ConvertParam;
import converttype.ReactClassToFuncProcess;
import converttype.ReactClassToVue3Process;
import converttype.Vue2ToReactFuncProcess;
import converttype.Vue2ToVue3Process;
import utils.ConvertLogUtil;
import utils.FileOperationUtil;
import utils.GenerateClassFileUtil;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConvertPanel extends JFrame {

	/**
	 * 前端框架vue和react版本升级处理工具
	 * 
	 * @author 郑荣鸿（ChengWingHung）
	 * @date 20231010 21:00:00 - 20231123 19:45:00
	 * @description 前端框架转换工具
	 * @version 1.0.0
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Object[] convertTypeList = {"---请选择---", "vue2升级vue3", "react类组件升级为函数组件"};
	
	private static Object[] fileTypeList = {"文件", "文件夹"};
	
	public JPanel lastLayoutPanel;
	
	private int processFileIndex = 0;// 执行文件的索引
	
	private int processFileTypeIndex;// 操作类型索引
	
	private ArrayList<String> fileList;// 选择的所有文件
	
	private ArrayList<String> copyFileList;// 直接拷贝到目标的文件
	
	private ArrayList<String> processFileList;// 需要解析的文件信息
	
	private ArrayList<String> errorFileList;// 解析异常的文件信息
	
	public int fileTypeIndex;// 选择文件类型
	
	public String selectedFileDir = "";// 选择的文件夹路径
	
	public JLabel outputFilePathLabel;// 输出文件路径信息
	
	public JLabel processResultLabel;// 显示文件处理信息
	
	private boolean hasSelectedFileFlg = false;// 是否已选择要执行的文件
	
	private long parseFileStartTime;
	
	public ConvertPanel()
    {
        setTitle(ConvertParam.TOOL_NAME + " " + ConvertParam.TOOL_VERSION);//设置显示窗口标题
        setSize(550,400);//设置窗口显示尺寸
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//置窗口是否可以关闭
        setLocationRelativeTo(null);//窗口在屏幕中间显示
        
        JPanel selectTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 操作类型选择
        
        JLabel typeLabel= new JLabel("操作类型：");
        JComboBox typeSelectBox = new JComboBox(convertTypeList);
        
        selectTypePanel.add(typeLabel);
        selectTypePanel.add(typeSelectBox);
        typeLabel.setSize(200, 35);
        selectTypePanel.setBounds(100, 40, 380, 35);
        
        JPanel selectFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 选择文件路径
        
        JLabel selectFileLabel= new JLabel("选择文件：");
        JComboBox fileTypeBox = new JComboBox(fileTypeList);
        JButton selectFileButton = new JButton("选择");
        fileTypeBox.setSize(250, 45);
        
        selectFilePanel.add(selectFileLabel); 
        selectFilePanel.add(fileTypeBox); 
        selectFilePanel.add(selectFileButton); 
        selectFilePanel.setBounds(100, 80, 380, 35);
        
        JPanel selectedFilePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 选择完的文件路径
        JLabel selectedFilePathLabel= new JLabel("执行路径：");
        JLabel selectedFilePathValueLabel= new JLabel("尚未选择文件！");
        selectedFilePathPanel.add(selectedFilePathLabel);
        selectedFilePathPanel.add(selectedFilePathValueLabel);
        selectedFilePathPanel.setBounds(100, 110, 380, 35);
        
        JPanel outputFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 输出文件路径设置
        
        JLabel outputLabel= new JLabel("输出路径：");
        JButton changePathButton = new JButton("修改");
        
        changePathButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

            	JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            	jfc.setDialogTitle("请选择输出文件路径");
                jfc.setAcceptAllFileFilterUsed(false);
                
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                int returnValue = jfc.showOpenDialog(null);
                
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                	
                	outputFilePathLabel.setText(jfc.getSelectedFile().getPath() + "/");
                    
                }
            }
        });
        
        outputFilePanel.add(outputLabel); 
        outputFilePanel.add(changePathButton);
        outputFilePanel.setBounds(100, 140, 380, 35);   
        
        JPanel outputFilePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 输出文件路径
        outputFilePathLabel = new JLabel(FileSystemView.getFileSystemView().getHomeDirectory().toString() + "/");// 设置默认输出目录
        outputFilePathPanel.add(outputFilePathLabel); 
        outputFilePathPanel.setBounds(100, 170, 380, 35);   
        
        JPanel excuteBtnPanel = new JPanel(new FlowLayout());// 执行操作       
        
        JButton excuteButton = new JButton("执行");
        excuteButton.setSize(100, 35);
        excuteButton.setBackground(Color.BLACK);
        
        excuteBtnPanel.add(excuteButton); 
        excuteBtnPanel.setBounds(100, 210, 380, 35);
        //excuteBtnPanel.setBackground(Color.BLUE);
        
        JPanel showProcessResultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 显示执行结果
        processResultLabel = new JLabel("当前执行内容信息");
        processResultLabel.setForeground(Color.RED);
        showProcessResultPanel.add(processResultLabel); 
        showProcessResultPanel.setBounds(100, 240, 380, 105);
        
        lastLayoutPanel = new JPanel(new FlowLayout());// 最后一个用于布局处理
        
        selectFileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

            	fileTypeIndex = fileTypeBox.getSelectedIndex();
            	
            	JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            	jfc.setDialogTitle("选择"+(fileTypeIndex == 0?"文件":"文件夹"));
                jfc.setAcceptAllFileFilterUsed(false);
                
                if (fileTypeIndex == 1) {
                	jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
                
                int returnValue = jfc.showOpenDialog(null);
                
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                	
                	errorFileList = new ArrayList<String>();
                	
                	if (fileTypeIndex == 1) {
                		if (jfc.getSelectedFile().isDirectory()) {
                			hasSelectedFileFlg = true;
                			fileList = FileOperationUtil.getProcessFileList(jfc.getSelectedFile());
                			selectedFileDir = jfc.getSelectedFile().toString() + "/";
                			selectedFilePathValueLabel.setText("已选择文件夹！");
                        }
                	} else {
                		hasSelectedFileFlg = true;
                		fileList = new ArrayList<String>();
                		fileList.add(jfc.getSelectedFile().getPath().toString());
                		selectedFilePathValueLabel.setText("已选择文件！");
                	}
                    
                }
                
            }
        });
        
        excuteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	
            	ConvertLogUtil.printConvertLog("info", "点击了执行操作");
                
                if (typeSelectBox.getSelectedIndex() < 1) {
                	JOptionPane.showMessageDialog(lastLayoutPanel, "请选择要操作的类型", "警告", JOptionPane.ERROR_MESSAGE);
                	return;
                }
                
                if (!hasSelectedFileFlg) {
                	JOptionPane.showMessageDialog(lastLayoutPanel, "请选择要执行的文件信息", "警告", JOptionPane.ERROR_MESSAGE);
                	return;
                }
                
                String showResult = "";
                String processFileType = "";
                
                processFileList = new ArrayList<String>();
                copyFileList = new ArrayList<String>();
                
                if (typeSelectBox.getSelectedIndex() == 1 || typeSelectBox.getSelectedIndex() == 3) {
                	processFileType = ".js,.vue";
                } else if (typeSelectBox.getSelectedIndex() == 2 || typeSelectBox.getSelectedIndex() == 4) {
                	processFileType = ".js,.jsx";
                }
                
                for (int i=0;i<fileList.size();i++) {
                	
                	showResult = fileList.get(i).toString();
                	
                	// 后缀为js/vue/jsx的需要处理
                	if (showResult.lastIndexOf('.') > -1 && processFileType.indexOf(showResult.substring(showResult.lastIndexOf('.'), showResult.length())) > -1) {
                		processFileList.add(showResult);
                	} else {
                		copyFileList.add(showResult);
                	}
                	
                }
                
                if (processFileList.size() == 0) {
                	JOptionPane.showMessageDialog(lastLayoutPanel, "选择的文件无需解析，请重新选择！", "警告", JOptionPane.ERROR_MESSAGE);
                	return;
                }
                
                processFileTypeIndex = typeSelectBox.getSelectedIndex();
                processFileIndex = 0;
                
                GenerateClassFileUtil.fileListMap = new HashMap<>();
                
                parseFileStartTime = new Date().getTime();
                
                readFileContentAndParse(processFileIndex);
                	
            }
        });
               
        add(selectTypePanel);
        add(selectFilePanel);
        add(selectedFilePathPanel);
        add(outputFilePanel);
        add(outputFilePathPanel);
        add(excuteBtnPanel);
        add(showProcessResultPanel);
        add(lastLayoutPanel);
        
        setVisible(true);//设置窗口是否可见
    }
	
	/**
	 * 读取文件内容开始解析
	 * 
	 */
	private void readFileContentAndParse(int fileIndex) {
		
		String outPutFileDir = outputFilePathLabel.getText() + ConvertParam.LOCAL_TEST_OUTPUT_FILE_PATH;
		
		if (processFileTypeIndex == 1 || processFileTypeIndex == 3) {
			
			outPutFileDir += ConvertParam.VUE3_OUTPUT_FILE_PATH + "/";
		} else if (processFileTypeIndex == 2 || processFileTypeIndex == 4) {
    		
			outPutFileDir += ConvertParam.REAT_OUTPUT_FILE_PATH + "/";
    	}
		
		// 判断是否解析完毕
		if (fileIndex == processFileList.size()) {
			
			// 需要添加的封装的文件
			for (Map.Entry<String, String> entry:GenerateClassFileUtil.fileListMap.entrySet()){
				
				try {
					
					FileOperationUtil.createResultFile(outPutFileDir, outPutFileDir + entry.getKey());// 创建生成的文件
		        	
		    		FileOperationUtil.writeContentIntoFile(outPutFileDir + entry.getKey(), entry.getValue(), false);//写入文件
				} catch(IOException err) {
					
		        	ConvertLogUtil.printConvertLog("error", "创建封装的文件失败:" + outPutFileDir + entry.getKey());
				}
				
			}
			
			// 此处后续可以考虑优化为全部解析完成后再生成文件，如果需要在解析某个文件中间过程中去增加其他文件的信息时
			
			ConvertLogUtil.printConvertLog("info", "所有文件解析完成");
			
			parseFileStartTime = new Date().getTime() - parseFileStartTime;
			
			String showResult = "<html><body>";
			
			if (fileTypeIndex == 1) {
				
				ConvertLogUtil.printConvertLog("info", "******************开始拷贝文件******************");
				
				FileOperationUtil.copySourceFileList(copyFileList, selectedFileDir, outPutFileDir);// 拷贝文件内容并显示转换结果信息
				
				ConvertLogUtil.printConvertLog("info", "******************文件拷贝完成******************");
				
				showResult = "<html><body>";
				
				showResult += "解析完成！总耗时" + parseFileStartTime + "毫秒！<br/>";
				
				showResult += "选择文件" + fileList.size() + "个;";
				
				showResult += "需解析" + processFileList.size() + "个;";
				
				showResult += "解析成功" + (processFileList.size() - errorFileList.size()) + "个;<br/>";
				
				showResult += "解析异常" + errorFileList.size() + "个;<br/>";
				
				showResult += "</body></html>";
			} else {
				
				if (errorFileList.size() == 0) {
					
					showResult = "解析成功！耗时" + parseFileStartTime + "毫秒！";
				} else {
					
					showResult = "解析失败！";
				}
			}
			
			showProcessContent(showResult, 1);
			return;
		}
		
		String relativeFilePath = "";
		String fileContentValue = "";
		String currentFilePath = processFileList.get(fileIndex).toString();
		
		String resultFilePath = "";
		String resultFileName = "";
		String parseResultFileContent = "";
    	
    	try {
    		fileContentValue = FileOperationUtil.readFileUsingInputStream(new File(processFileList.get(fileIndex).toString()));
    	} catch(IOException err) {
    		showProcessContent("文件读取异常：" + currentFilePath, 0);
    		errorFileList.add(currentFilePath);
    		processFileIndex++;
    		readFileContentAndParse(processFileIndex);
    		return;
    	}
    	
    	showProcessContent("当前处理文件：" + currentFilePath, 0);
    	
    	resultFilePath = currentFilePath;
		resultFileName = currentFilePath.lastIndexOf('/') > -1?currentFilePath.substring(currentFilePath.lastIndexOf('/') + 1, currentFilePath.length()):currentFilePath;
		
		ConvertLogUtil.printConvertLog("info", "当前执行的文件：" + resultFilePath);
		
		String outPutFilePath = "";
		
		if (fileTypeIndex == 1) {
			
			// 获取相对路径信息
			relativeFilePath = resultFilePath.substring(resultFilePath.indexOf(selectedFileDir) + selectedFileDir.length(), resultFilePath.length());
			
			outPutFilePath = outPutFileDir + relativeFilePath;
			
		} else {
			
			relativeFilePath = resultFileName;
			
			outPutFilePath = outPutFileDir + resultFileName;
		}
		
		try {
			
			if (processFileTypeIndex == 1) {
	    		
	    		// vue2 => vue3
	    		parseResultFileContent = Vue2ToVue3Process.parseVue2FileContent(resultFileName, relativeFilePath, fileContentValue);
	    	} else if (processFileTypeIndex == 2) {
	    		
	    		// react class => function
	    		parseResultFileContent = ReactClassToFuncProcess.parseReactFileContent(resultFileName, fileContentValue);
	    	} else if (processFileTypeIndex == 3) {
	    		
	    		// vue2 => react function 预留
	    		parseResultFileContent = Vue2ToReactFuncProcess.parseVue2FileContent(resultFileName, fileContentValue);
	    	} else if (processFileTypeIndex == 4) {
	    		
	    		// react class => vue3 预留
	    		parseResultFileContent = ReactClassToVue3Process.parseReactFileContent(resultFileName, fileContentValue);
	    	}
			
        	FileOperationUtil.createResultFile(outPutFileDir, outPutFilePath);// 创建生成的文件
        	
    		FileOperationUtil.writeContentIntoFile(outPutFilePath, parseResultFileContent, false);//写入文件
        	
        } catch(Exception err) {
			
        	errorFileList.add(currentFilePath);
        	
        	ConvertLogUtil.printConvertLog("error", "创建文件失败:" + resultFilePath + "\n异常原因:" + err.getMessage());
		}
    	
    	processFileIndex++;
		readFileContentAndParse(processFileIndex);
		
		// 后续改为多线程解析文件，提高解析速度
		
	}
	
	/**
	 * 显示处理结果信息
	 * 
	 * @param showResult
	 * 
	 */
	private void showProcessContent(String showResult, int type) {
		
		if (type == 0) {
			
			int subLength = 30;
			
			String temp = "";
			
			if (showResult.length() > subLength) {
	    		temp = showResult;
	    		showResult = "<html><body>";
	    		
	    		for (int j=0;j<temp.length();) {
	    			
	    			if (j+subLength < temp.length()) {
	    				showResult += temp.substring(j, j+subLength) + "<br/>";
	    			} else {
	    				showResult += temp.substring(j, temp.length());
	    			}
	    			
	    			j += subLength;
	    		}
	    		
	    		showResult += "</body></html>";
	    	}
		}
    	
    	processResultLabel.setText(showResult);
	}
	
    public static void main(String[] agrs)
    {
    	
        new ConvertPanel();//创建一个实例化对象
        
    }

}
