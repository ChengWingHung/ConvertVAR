import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileSystemView;

import converttype.Vue2ToVue3Process;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ConvertPanel extends JFrame {

	/**
	 * 前端框架vue和react版本升级及两大框架之间迭代转换工具
	 * 
	 * @author zhengronghong
	 * @date 20231010 21:00:00
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Object[] convertTypeList = {"---请选择---", "vue2升级为vue3", "react类组件升级为函数组件", "vue2迭代为react函数组件", "react类组件迭代为vue3"};
	
	private static Object[] fileTypeList = {"文件", "文件夹"};
	
	public JPanel lastLayoutPanel;
	
	private int processFileIndex = 0;// 执行文件的索引
	
	private int processFileTypeIndex;// 操作类型索引
	
	private ArrayList<String> fileList;// 选择的所有文件
	
	private ArrayList<String> copyFileList;// 直接拷贝到目标的文件
	
	private ArrayList<String> processFileList;// 需要解析的文件信息
	
	public JLabel processResultLabel;// 显示文件处理信息
	
	private boolean hasSelectedFileFlg = false;// 是否已选择要执行的文件
	
	public ConvertPanel()
    {
        setTitle("ConvertVAR v1.0.0");//设置显示窗口标题
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
        selectedFilePathPanel.setBounds(100, 130, 380, 35);
        
        JPanel outputFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));// 输出文件路径设置
        
        JLabel outputLabel= new JLabel("输出路径：");
        JTextField outputTextField = new JTextField(18);
        outputTextField.setText(FileSystemView.getFileSystemView().getHomeDirectory().toString() + "/");// 设置默认输出目录
        outputFilePanel.add(outputLabel); 
        outputFilePanel.add(outputTextField);
        outputFilePanel.setBounds(100, 170, 380, 35);       
        
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

            	int selectedIndex = fileTypeBox.getSelectedIndex();
            	
            	JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            	jfc.setDialogTitle("选择"+(selectedIndex == 0?"文件":"文件夹"));
                jfc.setAcceptAllFileFilterUsed(false);
                
                if (selectedIndex == 1) {
                	jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
                
                int returnValue = jfc.showOpenDialog(null);
                
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                	
                	if (selectedIndex == 1) {
                		if (jfc.getSelectedFile().isDirectory()) {
                			hasSelectedFileFlg = true;
                			fileList = new ArrayList<String>();
                			getProcessFileList(jfc.getSelectedFile());
                			selectedFilePathValueLabel.setText(jfc.getSelectedFile().toString() + "/");
                        }
                	} else {
                		hasSelectedFileFlg = true;
                		fileList = new ArrayList<String>();
                		fileList.add(jfc.getSelectedFile().getPath().toString());
                		selectedFilePathValueLabel.setText(jfc.getSelectedFile().getPath().toString());
                	}
                    
                }
                
            }
        });
        
        excuteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	
                System.out.println("点击了执行操作");
                
                if (typeSelectBox.getSelectedIndex() < 1) {
                	JOptionPane.showMessageDialog(lastLayoutPanel, "请选择要操作的类型", "警告", JOptionPane.ERROR_MESSAGE);
                	return;
                } else if (typeSelectBox.getSelectedIndex() > 1) {
                	JOptionPane.showMessageDialog(lastLayoutPanel, "目前仅支持vue2升级vue3", "警告", JOptionPane.ERROR_MESSAGE);
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
                
                readFileContentAndParse(processFileIndex);
                	
            }
        });
               
        add(selectTypePanel);
        add(selectFilePanel);
        add(selectedFilePathPanel);
        add(outputFilePanel);
        add(excuteBtnPanel);
        add(showProcessResultPanel);
        add(lastLayoutPanel);
        
        setVisible(true);//设置窗口是否可见
    }
	
	// 递归获取文件信息
	private void getProcessFileList(File fileDir) {
		
		File[] files = fileDir.listFiles();
		
		if (files != null) {
			
			for (File f : files) {
				if (f.isFile()) {
					fileList.add(f.toString());
				} else {
					getProcessFileList(f);
				}
			}
		}
	}
	
	// 读取文件内容
	public String readFileUsingInputStream(File file) throws IOException
	{
		InputStream ins = null;
		StringBuffer buffer = new StringBuffer();//用一个buffer保存读取的数据
	
		try{
			ins = new FileInputStream(file);//FileInputStream的构造方法，指定某个文件
		
			byte[] tmp = new byte[10];
		
			int length = 0;
		
			//int read(byte[] a)方法，从输入流读取数据，并存储于缓冲区a内，返回值为读取字节数
			//流读取最后完好的保存了换行 空格
			while((length = ins.read(tmp)) != -1)//每次读10字节
			{
				buffer.append(new String(tmp,0,length));
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(ins != null)
			{
				ins.close();
			}
		}
	
		return buffer.toString();
	}
	
	/**
	 * 读取文件内容开始解析
	 * 
	 */
	private void readFileContentAndParse(int fileIndex) {
		
		// 判断是否解析完毕
		if (fileIndex == processFileList.size()) {
			System.out.print("所有文件解析完成，准备生成最终的文件包");
			return;
		}
		
		String fileContentValue = "";
		String currentFilePath = processFileList.get(fileIndex).toString();
		
		String resultFilePath = "";
		String resultFileName = "";
		String parseResultFileContent = "";
    	
    	try {
    		fileContentValue = readFileUsingInputStream(new File(processFileList.get(fileIndex).toString()));
    	} catch(IOException err) {
    		showProcessContent("文件读取异常："+currentFilePath);
    		processFileIndex++;
    		readFileContentAndParse(processFileIndex);
    		return;
    	}
    	
    	showProcessContent("当前处理文件：" + currentFilePath);
    	
    	// 调用相应类型解析类
    	if (processFileTypeIndex == 1) {
    		
    		resultFilePath = currentFilePath;
    		resultFileName = currentFilePath.lastIndexOf('/') > -1?currentFilePath.substring(currentFilePath.lastIndexOf('/'), currentFilePath.length()):currentFilePath;
    		
    		parseResultFileContent = Vue2ToVue3Process.parseVue2FileContent(fileContentValue);//vue2->vue3
    	}
    	
    	processFileIndex++;
		readFileContentAndParse(processFileIndex);
		
		// 后续改为多线程解析文件，提高解析速度
		
	}
	
	/**
	 * 显示处理结果信息
	 * @param showResult
	 * 
	 */
	private void showProcessContent(String showResult) {
		
		int subLength = 40;
		
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
    	
    	processResultLabel.setText(showResult);
	}
	
    public static void main(String[] agrs)
    {
    	
        new ConvertPanel();//创建一个实例化对象
        
    }

}
