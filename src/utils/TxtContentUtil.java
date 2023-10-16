package utils;

public class TxtContentUtil {

	public TxtContentUtil() {
		
	}
	
	/**
	 * 获取html标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return
	 */
	public static String getHtmlTagProps(String sourceText, String propsName, String endProps) {
		
		
		String resultText = sourceText.substring(sourceText.indexOf(propsName) + propsName.length(), sourceText.length());
		
		resultText = resultText.substring(0, resultText.indexOf(endProps) + endProps.length() - 1);
		
		return resultText;
	}
	
	/**
	 * 清除html标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return
	 */
	public static String clearHtmlTagProps(String sourceText, String propsName, String endProps) {
		
		String resultText = sourceText.substring(sourceText.indexOf(propsName) + propsName.length(), sourceText.length());
		
		resultText = resultText.substring(0, resultText.indexOf(endProps) + endProps.length());
		resultText = sourceText.substring(resultText.indexOf(propsName) , resultText.indexOf(propsName) + propsName.length()) + resultText;
		
		return sourceText.replace(resultText, "");
	}
	
	/**
	 * 获取成对标签结束位置
	 * 
	 * @param sourceText
	 * @param startTag
	 * @param endTag
	 * @return
	 */
	public static int getTagEndIndex(String sourceText, char startTag, char endTag) {
		
		int startIndex = 0;
		int endIndex = 0;
		
		for (int i=0;i<sourceText.length();i++) {
			
			if (sourceText.charAt(i) == startTag) {
				startIndex++;
			}
			
			if (startTag == endTag) {
				if (startIndex == 2) {
					endIndex = i;
					break;
				}
			} else {
				
				if (sourceText.charAt(i) == endTag) {
					endIndex++;
				}
				
				if (startIndex != 0 && startIndex == endIndex) {
					endIndex = i;
					break;
				}
			}
		}
		
		return endIndex;
	}
	
	/**
	 * 获取语句结束索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getStatementEndIndex(String sourceText) {
		
		int index = -1;
		int i = 0;
		
		for (;i<sourceText.length();i++) {
			if (sourceText.charAt(i) == '\r' || sourceText.charAt(i) == '\t' || sourceText.charAt(i) == '\n' || sourceText.charAt(i) == ';') {
				if (i == 0 && sourceText.charAt(i) != ';') continue;
				index = i;
				break;
			}
		}
		
		return index == -1?i:index;
	}
}
