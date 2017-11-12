package com.mcslocation.excel;

import android.os.Environment;

import java.io.File;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class CreateExcel {
	// 准备设置excel工作表的标题
	private WritableSheet sheet;
	/**创建Excel工作薄*/
	private WritableWorkbook wwb;
	private String[] title = {"定位是否成功","手机型号","手机号码","定位类型","经度","纬度","精度","提供者","速度","角度","星级",
			"省","市","区","地址","近似点","时间","WIFI是否打开","GPS状态","GPS星数","错误码","错误信息","错误描述"};
	public CreateExcel() {
		excelCreate();
	}

	public void excelCreate() {
		try {
			/**输出的excel文件的路径*/
			String filePath = Environment.getExternalStorageDirectory() + "/location_bill";
			File file = new File(filePath, "location.xls");
			if (!file.exists()) {
				file.createNewFile();
			}
			wwb = Workbook.createWorkbook(file);
			/**添加第一个工作表并设置第一个Sheet的名字*/
			sheet = wwb.createSheet("定位地址信息表", 0);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveDataToExcel(int index, String[] content) throws Exception {
		Label label;
		for (int i = 0; i < title.length; i++) {
			/**Label(x,y,z)其中x代表单元格的第x+1列，第y+1行, 单元格的内容是y
			 * 在Label对象的子对象中指明单元格的位置和内容
			 * */
			label = new Label(i, 0, title[i]);
			/**将定义好的单元格添加到工作表中*/
			sheet.addCell(label);
		}
		/*
		 * 把数据填充到单元格中
		 * 需要使用jxl.write.Number
		 * 路径必须使用其完整路径，否则会出现错误
		 */
		for (int i = 0; i < title.length; i++) {
			Label labeli = new Label(i, index, content[i]);
			sheet.addCell(labeli);
		}
		// 写入数据
		wwb.write();
		// 关闭文件
		wwb.close();
	}

}
