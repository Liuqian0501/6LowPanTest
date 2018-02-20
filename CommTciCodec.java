package com.testingtech.ttcn.tci.codec;

import java.io.ByteArrayOutputStream;

import org.etsi.ttcn.tci.*;
import org.etsi.ttcn.tri.*;


import checksum.Checksum;

import com.testingtech.ttcn.tci.codec.base.AbstractBaseCodec;
import com.testingtech.ttcn.tri.TriMessageImpl;

public class CommTciCodec extends AbstractBaseCodec implements TciCDProvided {
	
	private String sourceAddress_s;
	private String destAddress_s;
	private boolean hasLowpan_NHC=false;
	private boolean hasNextHead=false;
	private boolean hasHLIM=true;
	private boolean hasFragmentOption=false;
	private boolean isDataGram_offset=false;
	private int datagram_size;
	private String HLIM;
	private String SAC;
	private String SAM;
	private String M;
	private String DAC;
	private String DAM;
	private boolean isIcmpv6RequestMessage=false;
	private boolean isIcmpv6ReplyMessage = false;
	private boolean isIcmpv6NSMessage=false;
	private boolean isIcmpv6RSMessage=false;
	private boolean isNoCompressIPv6=false;
	private boolean hasTF=false;
	private boolean isIcmpv6RAMessage=false;
	public CommTciCodec(de.tu_berlin.cs.uebb.muttcn.runtime.RB rb) { 
		super(rb);
		// TODO Auto-generated constructor stub
	}
	 
	public synchronized TriMessage encode(Value template)
	{
		//初始化
		sourceAddress_s=null;
		destAddress_s=null;
		hasLowpan_NHC=false;
		hasNextHead=true;  //注意，假定不省略
		hasHLIM=true;   //注意，假定不省略
		hasFragmentOption=false;
		isDataGram_offset=false;
		isIcmpv6RequestMessage=false;
		isIcmpv6NSMessage=false;
		isIcmpv6RSMessage=false;
		isIcmpv6RAMessage=false;
		isNoCompressIPv6=false;
		HLIM="'00'B";
		SAC="'0'B";
		SAM="'00'B";
		M="'0'B";
		DAC="'0'B";
		DAM="'00'B";
		
		if(template.getType().getName().equals("DataGram_Request"))  
		{
			System.out.println("--->开始编码");
			isIcmpv6RequestMessage=true;
			return encodeDataGram((RecordValue) template);
		}
		
		if(template.getType().getName().equals("DataGram_Fragmentation"))
		{
			System.out.println("--->开始编码");   //为分片报文，且是第一片
			isIcmpv6RequestMessage=true;
			return encodeDataGram((RecordValue) template);
		}
		
		if(template.getType().getName().equals("DataGram_offset"))
		{
			System.out.println("--->开始编码");
			isDataGram_offset=true;   //为分片报文，且不是第一片
			return encodeDataGram((RecordValue) template);
		}
		
		if(template.getType().getName().equals("DataGram_NS"))  //编码一个NS邻居请求报文
		{
			System.out.println("--->开始编码");
			isIcmpv6NSMessage=true;
			return encodeDataGram((RecordValue) template);
		}
		if(template.getType().getName().equals("DataGram_NA"))  //编码一个NS邻居通告报文
		{
			System.out.println("--->开始编码");
			isIcmpv6NSMessage=true;    //因为采用NS类似的报文，所以NA可以用NS的标记
			return encodeDataGram((RecordValue) template);
		}
		if(template.getType().getName().equals("DataGram_RS"))  //编码一个RS路由请求报文
		{
			System.out.println("--->开始编码DataGram_RS");
			isIcmpv6RSMessage=true;     //标记为RS报文
			return encodeDataGram((RecordValue) template);
		}
		if(template.getType().getName().equals("DataGram_RA"))  //编码一个RA路由通告报文
		{
			System.out.println("--->开始编码DataGram_RA");
			isIcmpv6RAMessage=true;     //标记为RA报文
			return encodeDataGram((RecordValue) template);
		}
		if(template.getType().getName().equals("DataGram_NoCompressIPv6"))  //编码一个不压缩IPv6请求回显报文
		{
			System.out.println("--->开始编码");
			isNoCompressIPv6=true;    //标记为不压缩的IPv6
			isIcmpv6RequestMessage=true;  //标记为携带ICMPv6回显请求报文
			return encodeDataGram((RecordValue) template);
		}
		if(template.getType().getName().equals("DataGram_NoCompressIPv6RS"))  //编码一个不压缩IPv6路由请求报文
		{
			System.out.println("--->开始编码");
			isNoCompressIPv6=true;    //标记为不压缩的IPv6
			isIcmpv6RSMessage=true;  //标记为携带ICMPv6路由请求报文
			return encodeDataGram((RecordValue) template);
		}
		return super.encode(template);
	}
	
	

	private TriMessage encodeDataGram(RecordValue template) {
		// TODO Auto-generated method stub
		
		bitpos = 0;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		if(isNoCompressIPv6){    //如果是不压缩IPv6的分派头
			BitstringValue lowPANDispatch = (BitstringValue)template.getField("lowPANDispatch");
			super.encodeBitstring(os, lowPANDispatch);
		}else{       //采用了IPHC分派头
			System.out.println("--->开始编码LowpanHeader");
			encodeLowpanHeader((RecordValue)template.getField("lowpanHeader"),os);
			System.out.println("--->完成编码LowpanHeader");
		}

		System.out.println("--->开始编码IPV6Header.");
		encodeIPV6Header((RecordValue)template.getField("ipv6Header"),os);
		System.out.println("--->完成编码IPV6Header");
		
		if(!hasNextHead){  //IPv6下一首部被省略，因此会有LOWPAN_NHC
			System.out.println("--->开始编码lowpan_NHC."); 
			encodeLowpan_NHC((RecordValue)template.getField("lowpan_NHC"),os);
			System.out.println("--->完成编码lowpan_NHC");
		}
		
		if(hasFragmentOption){
			System.out.println("--->开始编码fragmentOption."); 
			encodeFragmentOption((RecordValue)template.getField("fragmentOption"),os);
			System.out.println("--->完成编码fragmentOption");
		}
		if(!isDataGram_offset){
			
			if(isIcmpv6RequestMessage){			//如果是ICMPv6回显请求报文
				System.out.println("--->开始编码icmpv6RequestMessage.");
				encodeICMPV6RequestHeader((RecordValue)template.getField("icmpv6RequestMessage"),os);
				System.out.println("--->完成编码icmpv6RequestMessage.");
			}else if(isIcmpv6NSMessage){     //如果是ICMPv6邻居请求报文
				System.out.println("--->开始编码icmpv6NSMessage.");
				encodeICMPV6NS((RecordValue)template.getField("icmpv6NSMessage"),os);
				System.out.println("--->完成编码icmpv6NSMessage.");
			}else if(isIcmpv6RSMessage){			//如果是ICMPv6路由请求报文
				System.out.println("--->开始编码icmpv6RSMessage.");
				encodeICMPV6RS((RecordValue)template.getField("icmpv6RSMessage"),os);
				System.out.println("--->完成编码icmpv6RSMessage.");
			}else if(isIcmpv6RAMessage){			//如果是ICMPv6路由通告报文
				System.out.println("--->开始编码icmpv6RAMessage.");
				encodeICMPV6RA((RecordValue)template.getField("icmpv6RAMessage"),os);
				System.out.println("--->完成编码icmpv6RAMessage.");
			}
		}else{   //之后的分片，只有数据需要编码
			HexstringValue icmpv6Data = (HexstringValue)template.getField("icmpv6Data");
			super.encodeHexstring(os, icmpv6Data);
		}
		return new TriMessageImpl(os.toByteArray());
	}


	private void encodeICMPV6RA(RecordValue field, ByteArrayOutputStream os) {
		IntegerValue packetType = (IntegerValue)field.getField("packetType");	
		BitstringValue code = (BitstringValue)field.getField("code");
		BitstringValue validateSum = (BitstringValue)field.getField("validateSum");
		IntegerValue hopLimit_RA = (IntegerValue)field.getField("hopLimit_RA");
		BitstringValue m_RA = (BitstringValue)field.getField("m_RA");
		BitstringValue o_RA = (BitstringValue)field.getField("o_RA");
		BitstringValue reserve_RA = (BitstringValue)field.getField("reserve_RA");
		IntegerValue liveTime_RA = (IntegerValue)field.getField("liveTime_RA");	
		IntegerValue availableTime = (IntegerValue)field.getField("availableTime");	
		IntegerValue retryClock = (IntegerValue)field.getField("retryClock");	
		
		RecordValue icmpv6Option_SLLAO=null;
		IntegerValue optionType=null;
		IntegerValue length_SLLAO=null;
		HexstringValue srcLinkAddress=null;
		if(!field.getField("icmpv6Option_SLLAO").notPresent()){
			icmpv6Option_SLLAO =(RecordValue)field.getField("icmpv6Option_SLLAO");		
			optionType = (IntegerValue)icmpv6Option_SLLAO.getField("optionType");	
			length_SLLAO = (IntegerValue)icmpv6Option_SLLAO.getField("length_SLLAO");	
			srcLinkAddress = (HexstringValue)icmpv6Option_SLLAO.getField("srcLinkAddress");
		}
		
		RecordValue icmpv6Option_MTU=null;
		IntegerValue optionType_MTU=null;
		IntegerValue length_MTU=null;
		IntegerValue mtu=null;
		if(!field.getField("icmpv6Option_MTU").notPresent()){
			icmpv6Option_MTU =(RecordValue)field.getField("icmpv6Option_MTU");		
			optionType_MTU = (IntegerValue)icmpv6Option_MTU.getField("optionType");	
			length_MTU = (IntegerValue)icmpv6Option_MTU.getField("length_MTU");	
			mtu = (IntegerValue)icmpv6Option_MTU.getField("mtu");
		}
		
		//计算校验和 
		int packetType_i =packetType.getInt();
		String code_s=code.getString().substring(1, code.getString().length()-2);    
		String validateSum_s = validateSum.getString().substring(1, validateSum.getString().length()-2);
		int hopLimit_RA_i = hopLimit_RA.getInt();
		String m_RA_s = m_RA.getString().substring(1, m_RA.getString().length()-2);
		String o_RA_s = o_RA.getString().substring(1, o_RA.getString().length()-2);
		String reserve_RA_s = reserve_RA.getString().substring(1, reserve_RA.getString().length()-2);
		int liveTime_RA_i = liveTime_RA.getInt();
		int availableTime_i = availableTime.getInt();
		int retryClock_i = retryClock.getInt();
		
		
		int optionType_i=-1;
		int length_SLLAO_i=-1;
		String srcLinkAddress_s=null;
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
		optionType_i =optionType.getInt();
		length_SLLAO_i =length_SLLAO.getInt();
		srcLinkAddress_s = srcLinkAddress.getString().substring(1, srcLinkAddress.getString().length()-2);
		}
		
		int optionType_MTU_i=-1;
		int length_MTU_i=-1;
		int mtu_i=-1;
		if(optionType_MTU!=null&&length_MTU!=null&&mtu!=null){
			optionType_MTU_i =optionType_MTU.getInt();
			length_MTU_i =length_MTU.getInt();
			mtu_i = mtu.getInt();
		}
		
		 
		//计算完整的源IP地址和目的IP地址   
		if(srcLinkAddress_s!=null){
			getFullIpAddress(srcLinkAddress_s,"");	
		}
		
		Checksum checksum = new Checksum();
		int c=0;
		
		if(icmpv6Option_SLLAO!=null&&icmpv6Option_MTU!=null){
			System.out.println("计算包含SLLAO与MTU的校验和");
			c =checksum.checkSumIcmpv6RA(packetType_i, code_s,     //RA
					validateSum_s, hopLimit_RA_i,m_RA_s,o_RA_s,reserve_RA_s,liveTime_RA_i,availableTime_i,retryClock_i,
					 optionType_i,length_SLLAO_i, srcLinkAddress_s,optionType_MTU_i,length_MTU_i,mtu_i,sourceAddress_s, destAddress_s);
		}
		
		String s = Integer.toBinaryString(c);
		String s1=new String(s); //注意
		//补齐s到16位，注意             
		for(int i=1;i<=16-s.length();i++){
			s1 = "0"+s1; 
		}		
		validateSum.setString(s1);
		//计算校验和end
		
		encodeNumber(os,packetType);
		super.encodeBitstring(os, code);
		super.encodeBitstring(os, validateSum);
		encodeNumber(os,hopLimit_RA);
		super.encodeBitstring(os, m_RA);
		super.encodeBitstring(os, o_RA);
		super.encodeBitstring(os, reserve_RA);
		encodeNumber3(os, liveTime_RA);
		super.encodeInteger(os, availableTime);
		super.encodeInteger(os, retryClock);
		
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
			encodeNumber(os,optionType);
			encodeNumber(os,length_SLLAO);
			super.encodeHexstring(os, srcLinkAddress);		
		}	
		
		if(optionType_MTU!=null&&length_MTU!=null&&mtu!=null){
			encodeNumber(os,optionType_MTU);
			encodeNumber(os,length_MTU);
			encodeNumber4(os,mtu);
		}	
	}


	//编码ICMPv6的路由请求报文
	private void encodeICMPV6RS(RecordValue field, ByteArrayOutputStream os) {  
		IntegerValue packetType = (IntegerValue)field.getField("packetType");	
		BitstringValue code = (BitstringValue)field.getField("code");
		BitstringValue validateSum = (BitstringValue)field.getField("validateSum");
		BitstringValue reserve_32 = (BitstringValue)field.getField("reserve_32");
		
		RecordValue icmpv6Option_SLLAO=null;
		IntegerValue optionType=null;
		IntegerValue length_SLLAO=null;
		HexstringValue srcLinkAddress=null;
		if(!field.getField("icmpv6Option_SLLAO").notPresent()){
			icmpv6Option_SLLAO =(RecordValue)field.getField("icmpv6Option_SLLAO");		
			optionType = (IntegerValue)icmpv6Option_SLLAO.getField("optionType");	
			length_SLLAO = (IntegerValue)icmpv6Option_SLLAO.getField("length_SLLAO");	
			srcLinkAddress = (HexstringValue)icmpv6Option_SLLAO.getField("srcLinkAddress");
		}
		
		
		//计算校验和 
		int packetType_i =packetType.getInt();
		String code_s=code.getString().substring(1, code.getString().length()-2);    
		String validateSum_s = validateSum.getString().substring(1, validateSum.getString().length()-2);
		String reserve_32_s = reserve_32.getString().substring(1, reserve_32.getString().length()-2);
		
		
		int optionType_i=-1;
		int length_SLLAO_i=-1;
		String srcLinkAddress_s=null;
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
		optionType_i =optionType.getInt();
		length_SLLAO_i =length_SLLAO.getInt();
		srcLinkAddress_s = srcLinkAddress.getString().substring(1, srcLinkAddress.getString().length()-2);
		}
		
		 
		//计算完整的源IP地址和目的IP地址   
		if(srcLinkAddress_s!=null){
			getFullIpAddress(srcLinkAddress_s,"");	
		}
//		System.out.println(packetType_i); 
//		System.out.println(code_s); 
//		System.out.println(validateSum_s); 
//		System.out.println(reserve_32_s); 
//		System.out.println(optionType_i); 
//		System.out.println(length_SLLAO_i); 
//		System.out.println(srcLinkAddress_s); 		
//		System.out.println(sourceAddress_s+"   "+destAddress_s); 
		
		Checksum checksum = new Checksum();
		int c=0;
		
		if(icmpv6Option_SLLAO!=null){
			System.out.println("计算包含SLLAO的校验和");
			c =checksum.checkSumIcmpv6NS(packetType_i, code_s,     //复用了NS的校验和，不过目标地址置位空，因为RS没有该字段
					validateSum_s, reserve_32_s, "", optionType_i, 
					length_SLLAO_i, srcLinkAddress_s, sourceAddress_s, destAddress_s);
		}
		
		
		String s = Integer.toBinaryString(c);
		String s1=new String(s); //注意
		//补齐s到16位，注意             
		for(int i=1;i<=16-s.length();i++){
			s1 = "0"+s1; 
		}		
		validateSum.setString(s1);
		//计算校验和end
		
		encodeNumber(os,packetType);
		super.encodeBitstring(os, code);
		super.encodeBitstring(os, validateSum);
		super.encodeBitstring(os, reserve_32);
		
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
			encodeNumber(os,optionType);
			encodeNumber(os,length_SLLAO);
			super.encodeHexstring(os, srcLinkAddress);		
		}		
	}

	private void encodeICMPV6NS(RecordValue field, ByteArrayOutputStream os) {
		
		IntegerValue packetType = (IntegerValue)field.getField("packetType");	
		BitstringValue code = (BitstringValue)field.getField("code");
		BitstringValue validateSum = (BitstringValue)field.getField("validateSum");
		BitstringValue reserve_32 = (BitstringValue)field.getField("reserve_32");
		HexstringValue targetAddress = (HexstringValue)field.getField("targetAddress");
		
		RecordValue icmpv6Option_SLLAO=null;
		IntegerValue optionType=null;
		IntegerValue length_SLLAO=null;
		HexstringValue srcLinkAddress=null;
		if(!field.getField("icmpv6Option_SLLAO").notPresent()){
			icmpv6Option_SLLAO =(RecordValue)field.getField("icmpv6Option_SLLAO");		
			optionType = (IntegerValue)icmpv6Option_SLLAO.getField("optionType");	
			length_SLLAO = (IntegerValue)icmpv6Option_SLLAO.getField("length_SLLAO");	
			srcLinkAddress = (HexstringValue)icmpv6Option_SLLAO.getField("srcLinkAddress");
		}
		

		RecordValue icmpv6Option_ARO=null;
		IntegerValue optionType_ARO=null;
		IntegerValue length_ARO=null;
		BitstringValue status=null;
		BitstringValue reserve_24=null;
		IntegerValue liveTime=null;
		HexstringValue eui64=null;
		if(!field.getField("icmpv6Option_ARO").notPresent()){
			icmpv6Option_ARO =(RecordValue)field.getField("icmpv6Option_ARO");		
			optionType_ARO = (IntegerValue)icmpv6Option_ARO.getField("optionType");	
			length_ARO = (IntegerValue)icmpv6Option_ARO.getField("length_ARO");	
			status = (BitstringValue)icmpv6Option_ARO.getField("status");	
			reserve_24 = (BitstringValue)icmpv6Option_ARO.getField("reserve_24");
			liveTime = (IntegerValue)icmpv6Option_ARO.getField("liveTime");		
			eui64 = (HexstringValue)icmpv6Option_ARO.getField("eui64");
		}
		
		//计算校验和 
		int packetType_i =packetType.getInt();
		String code_s=code.getString().substring(1, code.getString().length()-2);    
		String validateSum_s = validateSum.getString().substring(1, validateSum.getString().length()-2);
		String reserve_32_s = reserve_32.getString().substring(1, reserve_32.getString().length()-2);
		String targetAddress_s = targetAddress.getString().substring(1, targetAddress.getString().length()-2);
		
		
		int optionType_i=-1;
		int length_SLLAO_i=-1;
		String srcLinkAddress_s=null;
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
		optionType_i =optionType.getInt();
		length_SLLAO_i =length_SLLAO.getInt();
		srcLinkAddress_s = srcLinkAddress.getString().substring(1, srcLinkAddress.getString().length()-2);
		}
		
		int optionType_ARO_i=-1;
		int length_ARO_i=-1;
		String status_s =null;
		String reserve_24_s =null;
		String liveTime_s =null;
		String eui64_s =null;
		
		if(optionType_ARO!=null&&length_ARO!=null&&status!=null&&reserve_24!=null&&liveTime!=null&&eui64!=null){
			optionType_ARO_i =optionType_ARO.getInt();
			length_ARO_i =length_ARO.getInt();
			 status_s = status.getString().substring(1, status.getString().length()-2);
			 reserve_24_s = reserve_24.getString().substring(1, reserve_24.getString().length()-2);
			 liveTime_s = Integer.toBinaryString(liveTime.getInt());
			 String temp= liveTime_s;
			 for(int i=0;i<16-liveTime_s.length();i++){
				 temp="0"+temp;
			 }
			 liveTime_s=temp;
			 System.out.println("liveTime_s="+liveTime_s);
			 eui64_s = eui64.getString().substring(1, eui64.getString().length()-2);
		}
		 
		//计算完整的源IP地址和目的IP地址  
		if(srcLinkAddress_s!=null){
			getFullIpAddress(srcLinkAddress_s,targetAddress_s);	
		}
		System.out.println(sourceAddress_s+"   "+destAddress_s); 
		
		Checksum checksum = new Checksum();
		int c=0;
		
		if(icmpv6Option_SLLAO!=null&&icmpv6Option_ARO==null){
			System.out.println("计算包含SLLAO的校验和");
			c =checksum.checkSumIcmpv6NS(packetType_i, code_s, validateSum_s, reserve_32_s, targetAddress_s, optionType_i, length_SLLAO_i, srcLinkAddress_s, sourceAddress_s, destAddress_s);
		}else if(icmpv6Option_SLLAO!=null&&icmpv6Option_ARO!=null){
			System.out.println("计算包含SLLAO与ARO的校验和");
			c =checksum.checkSumIcmpv6NA(packetType_i, code_s, validateSum_s, reserve_32_s, targetAddress_s, optionType_i, length_SLLAO_i, srcLinkAddress_s, 
					optionType_ARO_i,length_ARO_i,status_s,reserve_24_s,liveTime_s,eui64_s,sourceAddress_s, destAddress_s);
		}else{
			System.out.println("需要处理校验和的计算");
		}
		
		
		String s = Integer.toBinaryString(c);
		String s1=new String(s); //注意
		//补齐s到16位，注意             
		for(int i=1;i<=16-s.length();i++){
			s1 = "0"+s1; 
		}		
		validateSum.setString(s1);
		//计算校验和end
		
		encodeNumber(os,packetType);
		super.encodeBitstring(os, code);
		super.encodeBitstring(os, validateSum);
		super.encodeBitstring(os, reserve_32);
		super.encodeHexstring(os, targetAddress);
		
		if(optionType!=null&&length_SLLAO!=null&&srcLinkAddress!=null){
			encodeNumber(os,optionType);
			encodeNumber(os,length_SLLAO);
			super.encodeHexstring(os, srcLinkAddress);		
		}
		if(optionType_ARO!=null&&length_ARO!=null&&status!=null&&reserve_24!=null&&liveTime!=null&&eui64!=null){
			encodeNumber(os,optionType_ARO);
			encodeNumber(os,length_ARO);
			super.encodeBitstring(os, status);
			super.encodeBitstring(os, reserve_24);
			encodeNumber3(os,liveTime);   //将整数变为16比特
			super.encodeHexstring(os, eui64);
		}
	}

	private void getFullIpAddress(String srcLinkAddress_s,String targetAddress_s) {
		//计算完整的源IP地址和目的IP地址
		if("'0'B".equals(SAC)){
			if("'00'B".equals(SAM)){
				System.out.println("地址完整，不需要计算");
			}else if("'01'B".equals(SAM)){
				sourceAddress_s="FE80000000000000"+sourceAddress_s;
			}else if("'10'B".equals(SAM)){
				sourceAddress_s="FE80000000000000000000FFFE00"+sourceAddress_s;
			}else if("'11'B".equals(SAM)){
				sourceAddress_s="FE8000000000000002"+srcLinkAddress_s.substring(2, 16);
			}
		}else if("'1'B".equals(SAC)){
			if("'00'B".equals(SAM)){
				System.out.println("暂未指定");
			}else if("'01'B".equals(SAM)){
				
			}else if("'10'B".equals(SAM)){
				
			}else if("'11'B".equals(SAM)){
				
			}
		}
		
		if("'0'B".equals(M)){
			if("'0'B".equals(DAC)){
				if("'00'B".equals(DAM)){
					System.out.println("地址完整，不需要计算");
				}else if("'01'B".equals(DAM)){
					destAddress_s="FE80000000000000"+destAddress_s;
				}else if("'10'B".equals(DAM)){
					destAddress_s="FE80000000000000000000FFFE00"+destAddress_s;
				}else if("'11'B".equals(DAM)){
					destAddress_s=targetAddress_s;
				}
			}else if("'1'B".equals(DAC)){
				if("'00'B".equals(DAM)){
					
				}else if("'01'B".equals(DAM)){
					
				}else if("'10'B".equals(DAM)){
					
				}else if("'11'B".equals(DAM)){
					
				}
			}
		}else if("'1'B".equals(M)){
			if("'0'B".equals(DAC)){
				if("'00'B".equals(DAM)){
					System.out.println("地址完整，不需要计算");
				}else if("'01'B".equals(DAM)){
					String temp ="FF";
					temp=temp+destAddress_s.substring(0, 2)+"000000000000000000"+destAddress_s.substring(2,12);
					destAddress_s=temp;
				}else if("'10'B".equals(DAM)){
					
				}else if("'11'B".equals(DAM)){
					destAddress_s="FF0200000000000000000000000000"+destAddress_s;      //8位
				}
			}else if("'1'B".equals(DAC)){
				if("'00'B".equals(DAM)){
					
				}else if("'01'B".equals(DAM)){
					
				}else if("'10'B".equals(DAM)){
					
				}else if("'11'B".equals(DAM)){
					
				}
			}
		}
	}


	private void encodeLowpan_NHC(RecordValue field, ByteArrayOutputStream os) {
		BitstringValue fixedValue_Fragment = (BitstringValue)field.getField("fixedValue_Fragment");
		super.encodeBitstring(os, fixedValue_Fragment);
		
		BitstringValue cid_Fragment = (BitstringValue)field.getField("cid_Fragment");
		super.encodeBitstring(os, cid_Fragment);
		if("'010'B".equals(cid_Fragment.getString())){   //扩展选项为 分片选项
			hasFragmentOption = true;
		}
		
		BitstringValue nh_Fragment = (BitstringValue)field.getField("nh_Fragment");
		super.encodeBitstring(os, nh_Fragment);	
	}

	private void encodeFragmentOption(RecordValue field,
			ByteArrayOutputStream os) {
		
//		IntegerValue length_Fragment = (IntegerValue)field.getField("length_Fragment");	
//		encodeNumber(os,length_Fragment);
		
		IntegerValue nextHeader_Fragment = (IntegerValue)field.getField("nextHeader_Fragment");	
		encodeNumber(os,nextHeader_Fragment);
		
		BitstringValue reserve_a = (BitstringValue)field.getField("reserve_a");
		super.encodeBitstring(os, reserve_a);
		
		BitstringValue offset = (BitstringValue)field.getField("offset");
		super.encodeBitstring(os, offset);
		
		BitstringValue reserve_b = (BitstringValue)field.getField("reserve_b");
		super.encodeBitstring(os, reserve_b);
		
		BitstringValue m_Fragment = (BitstringValue)field.getField("m_Fragment");
		super.encodeBitstring(os, m_Fragment);
		
		BitstringValue identification = (BitstringValue)field.getField("identification");
		super.encodeBitstring(os, identification);
		
	}

	//将一个整数转变为13比特
	private void encodeNumber2(ByteArrayOutputStream os, IntegerValue offset) {
		int i = offset.getInt();
		byte[] b = new byte[1];
		b[0] = (byte)(i);
		os.write(b, 0, 1);
		bitpos += ((b.length) << 3);		
	}

	//将一个整数转变为16比特
	private void encodeNumber3(ByteArrayOutputStream os, IntegerValue offset) {
		int i = offset.getInt();
		byte[] b = new byte[2];
		b[0] = (byte)((i&0xffff)>>8);   
		b[1] = (byte)((i&0xffff));  
		os.write(b, 0, 2);
		bitpos += ((b.length) << 3);	
	}
	
	//将一个整数转变为48比特
	private void encodeNumber4(ByteArrayOutputStream os, IntegerValue offset) {
		int i = offset.getInt();
		byte[] b = new byte[6];
		b[0] = (byte)(0); 
		b[1] = (byte)(0); 
		b[2] = (byte)((i&0xffff)>>24); 
		b[3] = (byte)((i&0xffff)>>16); 
		b[4] = (byte)((i&0xffff)>>8);   
		b[5] = (byte)((i&0xffff));  
		os.write(b, 0, 6);
		bitpos += ((b.length) << 3);	
	}
	
	
	private void encodeLowpanHeader(RecordValue field, ByteArrayOutputStream os) {
		// TODO Auto-generated method stub
		BitstringValue fixedValue = (BitstringValue)field.getField("fixedValue");
		super.encodeBitstring(os, fixedValue);
		BitstringValue tf = (BitstringValue)field.getField("tf");
		super.encodeBitstring(os, tf);
		
		
		BitstringValue nh = (BitstringValue)field.getField("nh");
		super.encodeBitstring(os, nh);
		//判断IPv6首部的下一首部字段是否省略
		if("'0'B".equals(nh.getString())){  //未省略
			hasNextHead = true;
		}
		
		BitstringValue hlim = (BitstringValue)field.getField("hlim");
		super.encodeBitstring(os, hlim);
		//判断IPv6首部的 跳数限制 是否省略
		if("'11'B".equals(hlim.getString())){  //省略
			hasHLIM = false;   //省略该字段
		}
		
		BitstringValue cid = (BitstringValue)field.getField("cid");
		super.encodeBitstring(os, cid);
		
		
		BitstringValue sac = (BitstringValue)field.getField("sac");
		super.encodeBitstring(os, sac);
		SAC=sac.getString();
		
		BitstringValue sam = (BitstringValue)field.getField("sam");
		super.encodeBitstring(os, sam);
		SAM=sam.getString();
		
		BitstringValue m = (BitstringValue)field.getField("m");
		super.encodeBitstring(os, m);
		M=m.getString();
		
		BitstringValue dac = (BitstringValue)field.getField("dac");
		super.encodeBitstring(os, dac);
		DAC=dac.getString();
		
		BitstringValue dam = (BitstringValue)field.getField("dam");
		super.encodeBitstring(os, dam);
		DAM=dam.getString();
	}

	private void encodeICMPV6RequestHeader(RecordValue recordValue,
			ByteArrayOutputStream os) {
		//此处还有待考虑，如果ICMPv6分片了，那么目前的校验和算法就只计算了第一个分片中的数据，校验和不对
		IntegerValue packetType = (IntegerValue)recordValue.getField("packetType");	
		BitstringValue code = (BitstringValue)recordValue.getField("code");
		BitstringValue validateSum = (BitstringValue)recordValue.getField("validateSum");
		BitstringValue identifier = (BitstringValue)recordValue.getField("identifier");
		BitstringValue serialNumber = (BitstringValue)recordValue.getField("serialNumber");
		HexstringValue icmpv6Data = (HexstringValue)recordValue.getField("icmpv6Data");
		
		
		//计算校验和 
		int packetType_i =packetType.getInt();
		String code_s=code.getString().substring(1, code.getString().length()-2);    
		String validateSum_s = validateSum.getString().substring(1, validateSum.getString().length()-2);
		String identifier_s = identifier.getString().substring(1, identifier.getString().length()-2);
		String serialNumber_s = serialNumber.getString().substring(1, serialNumber.getString().length()-2);
		String icmpv6Data_s = icmpv6Data.getString().substring(1, icmpv6Data.getString().length()-2);		
		Checksum checksum = new Checksum();
		int c =checksum.checkSumIcmpv6(packetType_i, code_s, validateSum_s, identifier_s, serialNumber_s, icmpv6Data_s, sourceAddress_s, destAddress_s);
		String s = Integer.toBinaryString(c);
		String s1=new String(s); //注意
		//补齐s到16位，注意             
		for(int i=1;i<=16-s.length();i++){
			s1 = "0"+s1; 
		}	
		if("'0000000000000000'B".equals(validateSum.getString())){     //只有当校验和未指定时，才做计算
			validateSum.setString(s1);
		}
		//计算校验和end
		
		encodeNumber(os,packetType);
		super.encodeBitstring(os, code);
		super.encodeBitstring(os, validateSum);
		super.encodeBitstring(os, identifier);
		super.encodeBitstring(os, serialNumber);
		super.encodeHexstring(os, icmpv6Data);
	}

	private void encodeIPV6Header(RecordValue field, ByteArrayOutputStream os) {
		// TODO Auto-generated method stub
		if(isNoCompressIPv6){
			if(!field.getField("ipv6Version").notPresent()){    //如果ipv6Version没有省略
				BitstringValue ipv6Version = (BitstringValue)field.getField("ipv6Version");
				super.encodeBitstring(os, ipv6Version);
			}
			
			if(!field.getField("trafficType").notPresent()){    //如果trafficType没有省略
				BitstringValue trafficType = (BitstringValue)field.getField("trafficType");
				super.encodeBitstring(os, trafficType);
			}
			
			if(!field.getField("flow").notPresent()){    //如果flow没有省略
				BitstringValue flow = (BitstringValue)field.getField("flow");
				super.encodeBitstring(os, flow);
			}
			
			if(!field.getField("length_Load").notPresent()){    //如果length_Load没有省略
				IntegerValue length_Load = (IntegerValue)field.getField("length_Load");
				encodeNumber3(os,length_Load);    //16比特 
			}
		}
		
		if(!field.getField("nextHeader").notPresent()){    //如果nextHeader没有省略
			IntegerValue nextHeader = (IntegerValue)field.getField("nextHeader");
			encodeNumber(os,nextHeader);
		}		
		
		if(!field.getField("hopLimit").notPresent()){    //如果hopLimit没有省略
			IntegerValue hopLimit = (IntegerValue)field.getField("hopLimit");
			encodeNumber(os,hopLimit);
		}
		
		if(!field.getField("sourceAddress").notPresent()){    //如果sourceAddress没有省略
			HexstringValue sourceAddress = (HexstringValue)field.getField("sourceAddress");
			//System.out.println(sourceAddress.getString()+ "   " +sourceAddress.getLength());
			sourceAddress_s = sourceAddress.getString().substring(1, sourceAddress.getString().length()-2);
			super.encodeHexstring(os, sourceAddress);
		}
		
		if(!field.getField("destinationAddress").notPresent()){    //如果destinationAddress没有省略
			HexstringValue destinationAddress = (HexstringValue)field.getField("destinationAddress");
			destAddress_s =  destinationAddress.getString().substring(1, destinationAddress.getString().length()-2);
			//System.out.println(destinationAddress.getString()+ "   " +destinationAddress.getLength());
			super.encodeHexstring(os, destinationAddress);
		}
	}

	private void encodeNumber(ByteArrayOutputStream os, IntegerValue intValue) {
		// TODO Auto-generated method stub
		int i = intValue.getInt();
		byte[] b = new byte[1];
		b[0] = (byte)(i);
		os.write(b, 0, 1);
		bitpos += ((b.length) << 3);
	}
	
	public synchronized Value decode(TriMessage message, Type decodingHypothesis)
	{
		
		//初始化
		sourceAddress_s=null;
		destAddress_s=null;
		hasLowpan_NHC=false;
		hasNextHead=true;
		hasHLIM=true;   //注意，假定不省略
		hasFragmentOption=false;
		isDataGram_offset=false;
		isIcmpv6RequestMessage=false;
		isIcmpv6ReplyMessage=false;
		isIcmpv6NSMessage=false;
		isIcmpv6RAMessage=false;
		isIcmpv6RSMessage=false;
		datagram_size=0;
		isNoCompressIPv6=false;
		hasTF=false;   //注意，假定省略
		hasNextHead=true;
		hasHLIM=true;
		HLIM="'00'B";
		SAC="'0'B";
		SAM="'00'B";
		M="'0'B";
		DAC="'0'B";
		DAM="'00'B";

		
		System.out.println("<----开始解码");
		bitpos = 0; 
		
		if(decodingHypothesis.getName().equals("DataGram_Request"))
		{
			
			isIcmpv6RequestMessage=true;    //为ICMPv6回显请求报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_Reply"))
		{
			System.out.println("DataReply");
			isIcmpv6ReplyMessage=true;    //判断其是否为ICMPv6回显应答报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_Fragmentation"))
		{
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_offset"))
		{
			isDataGram_offset=true;
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		
		if(decodingHypothesis.getName().equals("DataGram_NS"))
		{
			isIcmpv6NSMessage=true;    //为ICMPv6邻居请求报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		
		if(decodingHypothesis.getName().equals("DataGram_NA"))
		{
			isIcmpv6NSMessage=true;    //为ICMPv6邻居通告报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_NoCompressIPv6"))
		{
			isNoCompressIPv6 = true;     //为未压缩的IPv6
			hasTF=true;  //不省略流量类型和流标签
			isIcmpv6RequestMessage=true;    //为ICMPv6回显应答报文,由于回显请求和应答格式相似，在此复用该标记
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_RA"))
		{
			isIcmpv6RAMessage=true;    //为ICMPv6路由通告报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		if(decodingHypothesis.getName().equals("DataGram_RS"))
		{
			isIcmpv6RSMessage=true;    //为ICMPv6路由请求报文
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			//System.out.println("报文长度为:"+encodedMessage1.length);
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeDataGram(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		
		
		if(decodingHypothesis.getName().equals("OtherPacket"))
		{
			RecordValue result = (RecordValue)decodingHypothesis.newInstance();
			byte[] encodedMessage1 = message.getEncodedMessage();
			
			//获得报文总长度
			datagram_size=encodedMessage1[0]&0xff;
			
			byte[] encodedMessage = new byte[encodedMessage1.length-1]; 
			System.arraycopy(encodedMessage1, 1, encodedMessage, 0, encodedMessage1.length-1);
			
			decodeOtherPacket(encodedMessage,result);
			System.out.println("<----结束解码");
			return result;
		}
		return super.decode(message, decodingHypothesis);
		
	}

	private void decodeOtherPacket(byte[] encodedMessage, RecordValue result) {
		System.out.println("<----开始解码allData");
		
		//计算数据部分长度,单位为比特
		int len = datagram_size*8;
		
		HexstringValue allData = super.createHexstringValue(encodedMessage, len);  //注意，此处长度应该是变长
		result.setField("allData", allData);
		System.out.println("<----结束解码allData");
	}

	private void decodeDataGram(byte[] encodedMessage, RecordValue result) {
		
		if(isNoCompressIPv6){    //未压缩的IPv6
			System.out.println("开始解码lowPANDispatch");
			BitstringValue lowPANDispatch = super.createBitstringValue(encodedMessage, 8);
			result.setField("lowPANDispatch", lowPANDispatch);
			System.out.println("结束解码lowPANDispatch");
			
			
		}else{     //使用 IPHC压缩IPv6
			System.out.println("<----开始解码LowpanHeader");
			RecordValue lowpanHeader = (RecordValue)result.getField("lowpanHeader");
			decodeLowpanHeader(encodedMessage,lowpanHeader);
			result.setField("lowpanHeader", lowpanHeader);
			System.out.println("<----结束解码LowpanHeader");			
		}
		
		
		System.out.println("<----开始解码IPV6Header...");
		RecordValue ipv6Header = (RecordValue)result.getField("ipv6Header");
		decodeIPV6Header(encodedMessage,ipv6Header);
		result.setField("ipv6Header", ipv6Header);
		System.out.println("<----结束解码IPV6Header...");
		
		if(!hasNextHead){   //IPv6下一首部被省略，因此会有LOWPAN_NHC
			System.out.println("<----开始解码lowpan_NHC...");
			RecordValue lowpan_NHC = (RecordValue)result.getField("lowpan_NHC");
			decodeLowpan_NHC(encodedMessage,lowpan_NHC);
			result.setField("lowpan_NHC", lowpan_NHC);
			System.out.println("<----结束解码lowpan_NHC...");
		}
		
		if(hasFragmentOption){    //有分片选项
			System.out.println("<----开始解码fragmentOption...");
			RecordValue fragmentOption = (RecordValue)result.getField("fragmentOption");
			decodeFragmentOption(encodedMessage,fragmentOption);
			result.setField("fragmentOption", fragmentOption);
			System.out.println("<----结束解码fragmentOption...");
		}
		if(!isDataGram_offset){		
			if(isIcmpv6RequestMessage){   //ICMPv6 回显请求报文
				System.out.println("<----开始解码icmpv6RequestMessage...>");
				RecordValue icmpv6RequestMessage = (RecordValue)result.getField("icmpv6RequestMessage"); 
				decodeICMPV6RequestOrReplyMessage(encodedMessage,icmpv6RequestMessage);
				result.setField("icmpv6RequestMessage", icmpv6RequestMessage);
				System.out.println("<----结束解码icmpv6RequestMessage>");
			}
			if(isIcmpv6ReplyMessage){    //ICMPv6回显应答报文
				System.out.println("<----开始解码icmpv6ReplyMessage...>");
				RecordValue icmpv6ReplyMessage = (RecordValue)result.getField("icmpv6ReplyMessage");
				decodeICMPV6RequestOrReplyMessage(encodedMessage,icmpv6ReplyMessage);
				result.setField("icmpv6ReplyMessage", icmpv6ReplyMessage);
				System.out.println("<----结束解码icmpv6ReplyMessage>");
			}
			if(isIcmpv6NSMessage){     //ICMPv6邻居请求报文		  		
				System.out.println("<----开始解码icmpv6NSMessage...");
				RecordValue icmpv6NSMessage = (RecordValue)result.getField("icmpv6NSMessage"); 
				decodeICMPv6NS(encodedMessage,icmpv6NSMessage);
				result.setField("icmpv6NSMessage", icmpv6NSMessage);
				System.out.println("<----结束解码icmpv6NSMessage");
			}
			if(isIcmpv6RAMessage){     //ICMPv6路由通告报文		  		
				System.out.println("<----开始解码icmpv6RAMessage...");
				RecordValue icmpv6RAMessage = (RecordValue)result.getField("icmpv6RAMessage"); 
				decodeICMPv6RA(encodedMessage,icmpv6RAMessage);
				result.setField("icmpv6RAMessage", icmpv6RAMessage);
				System.out.println("<----结束解码icmpv6RAMessage");
			}
			if(isIcmpv6RSMessage){     //ICMPv6路由请求报文		  		
				System.out.println("<----开始解码icmpv6RSMessage...");
				RecordValue icmpv6RSMessage = (RecordValue)result.getField("icmpv6RSMessage"); 
				decodeICMPv6RS(encodedMessage,icmpv6RSMessage);
				result.setField("icmpv6RSMessage", icmpv6RSMessage);
				System.out.println("<----结束解码icmpv6RSMessage");
			}
		}else{
			System.out.println("<----开始解码icmpv6Data");
			
			//计算ICMPv6的数据部分长度,单位为比特
			int len = datagram_size*8-bitpos;
			
			HexstringValue icmpv6Data = super.createHexstringValue(encodedMessage, len);  //注意，此处长度应该是变长
			result.setField("icmpv6Data", icmpv6Data);
			System.out.println("<----结束解码icmpv6Data");
		}
	}

	
	private void decodeICMPv6RS(byte[] encodedMessage,
			RecordValue icmpv6rsMessage) {
		System.out.println("Start to decode packetType");
		IntegerValue packetType = decodeNumber(encodedMessage);
		//System.out.println(packetType.getInt());
		icmpv6rsMessage.setField("packetType", packetType);
		System.out.println("Finish to decode packetType");
		
		System.out.println("Start to decode code");
		BitstringValue code = super.createBitstringValue(encodedMessage, 8);
		icmpv6rsMessage.setField("code", code);
		System.out.println("Finish to decode code");
		
		System.out.println("Start to decode validateSum");
		BitstringValue validateSum = super.createBitstringValue(encodedMessage, 16);
		icmpv6rsMessage.setField("validateSum", validateSum);
		System.out.println("Finish to decode validateSum");
		
		System.out.println("Start to decode reserve_32");
		BitstringValue reserve_32 = super.createBitstringValue(encodedMessage, 32);
		icmpv6rsMessage.setField("reserve_32", reserve_32);
		System.out.println("Finish to decode reserve_32");
		
		System.out.println("<----开始解码icmpv6Option_SLLAO...");
		RecordValue icmpv6Option_SLLAO = (RecordValue)icmpv6rsMessage.getField("icmpv6Option_SLLAO"); 
		decodeIcmpv6Option_SLLAO(encodedMessage,icmpv6Option_SLLAO);
		icmpv6rsMessage.setField("icmpv6Option_SLLAO", icmpv6Option_SLLAO);
		System.out.println("<----结束解码icmpv6Option_SLLAO");
		
	}

	private void decodeICMPv6RA(byte[] encodedMessage,
			RecordValue icmpv6raMessage) {
		System.out.println("Start to decode packetType");
		IntegerValue packetType = decodeNumber(encodedMessage);
		//System.out.println(packetType.getInt());
		icmpv6raMessage.setField("packetType", packetType);
		System.out.println("Finish to decode packetType");
		
		
		System.out.println("Start to decode code");
		BitstringValue code = super.createBitstringValue(encodedMessage, 8);
		icmpv6raMessage.setField("code", code);
		System.out.println("Finish to decode code");
		
		
		System.out.println("Start to decode validateSum");
		BitstringValue validateSum = super.createBitstringValue(encodedMessage, 16);
		icmpv6raMessage.setField("validateSum", validateSum);
		System.out.println("Finish to decode validateSum");
		
		
		System.out.println("Start to decode hopLimit_RA");
		IntegerValue hopLimit_RA = decodeNumber(encodedMessage);
		//System.out.println(packetType.getInt());
		icmpv6raMessage.setField("hopLimit_RA", hopLimit_RA);
		System.out.println("Finish to decode hopLimit_RA");
		
		System.out.println("Start to decode m_RA");
		BitstringValue m_RA = super.createBitstringValue(encodedMessage, 1);
		icmpv6raMessage.setField("m_RA", m_RA);
		System.out.println("Finish to decode m_RA");
		
		System.out.println("Start to decode o_RA");
		BitstringValue o_RA = super.createBitstringValue(encodedMessage, 1);
		icmpv6raMessage.setField("o_RA", o_RA);
		System.out.println("Finish to decode o_RA");
		
		System.out.println("Start to decode reserve_RA");
		BitstringValue reserve_RA = super.createBitstringValue(encodedMessage, 6);
		icmpv6raMessage.setField("reserve_RA", reserve_RA);
		System.out.println("Finish to decode reserve_RA");
		
		System.out.println("Start to decode liveTime_RA");
		IntegerValue liveTime_RA = decodeNumber2(encodedMessage);
		icmpv6raMessage.setField("liveTime_RA", liveTime_RA);
		System.out.println("Finish to decode liveTime_RA");
		
		System.out.println("Start to decode availableTime");
		IntegerValue availableTime = decodeNumber3(encodedMessage);
		icmpv6raMessage.setField("availableTime", availableTime);
		System.out.println("Finish to decode availableTime");
		
		System.out.println("Start to decode retryClock");
		IntegerValue retryClock = decodeNumber3(encodedMessage);
		icmpv6raMessage.setField("retryClock", retryClock);
		System.out.println("Finish to decode retryClock");
		
		System.out.println("<----开始解码icmpv6Option_SLLAO...");
		RecordValue icmpv6Option_SLLAO = (RecordValue)icmpv6raMessage.getField("icmpv6Option_SLLAO"); 
		decodeIcmpv6Option_SLLAO(encodedMessage,icmpv6Option_SLLAO);
		icmpv6raMessage.setField("icmpv6Option_SLLAO", icmpv6Option_SLLAO);
		System.out.println("<----结束解码icmpv6Option_SLLAO");
		
		System.out.println("<----开始解码icmpv6Option_MTU...");
		RecordValue icmpv6Option_MTU = (RecordValue)icmpv6raMessage.getField("icmpv6Option_MTU"); 
		decodeIcmpv6Option_MTU(encodedMessage,icmpv6Option_MTU);
		icmpv6raMessage.setField("icmpv6Option_MTU", icmpv6Option_MTU);
		System.out.println("<----结束解码icmpv6Option_MTU");
		
	}



	private void decodeICMPv6NS(byte[] encodedMessage,
			RecordValue icmpv6nsMessage) {
		
		System.out.println("Start to decode packetType");
		IntegerValue packetType = decodeNumber(encodedMessage);
		//System.out.println(packetType.getInt());
		icmpv6nsMessage.setField("packetType", packetType);
		System.out.println("Finish to decode packetType");
		
		
		System.out.println("Start to decode code");
		BitstringValue code = super.createBitstringValue(encodedMessage, 8);
		icmpv6nsMessage.setField("code", code);
		System.out.println("Finish to decode code");
		
		
		System.out.println("Start to decode validateSum");
		BitstringValue validateSum = super.createBitstringValue(encodedMessage, 16);
		icmpv6nsMessage.setField("validateSum", validateSum);
		System.out.println("Finish to decode validateSum");
		
		System.out.println("Start to decode reserve_32");
		BitstringValue reserve_32 = super.createBitstringValue(encodedMessage, 32);
		icmpv6nsMessage.setField("reserve_32", reserve_32);
		System.out.println("Finish to decode reserve_32");
		
		
		System.out.println("Start to decode targetAddress");
		HexstringValue targetAddress = super.createHexstringValue(encodedMessage, 128);
		icmpv6nsMessage.setField("targetAddress", targetAddress);
		System.out.println("Finish to decode targetAddress");
		
		System.out.println("<----开始解码icmpv6Option_SLLAO...");
		RecordValue icmpv6Option_SLLAO = (RecordValue)icmpv6nsMessage.getField("icmpv6Option_SLLAO"); 
		decodeIcmpv6Option_SLLAO(encodedMessage,icmpv6Option_SLLAO);
		icmpv6nsMessage.setField("icmpv6Option_SLLAO", icmpv6Option_SLLAO);
		System.out.println("<----结束解码icmpv6Option_SLLAO");
		
	}

	
	
	private void decodeIcmpv6Option_SLLAO(byte[] encodedMessage,
			RecordValue icmpv6Option_SLLAO) {
		System.out.println("Start to decode optionType");
		IntegerValue optionType = decodeNumber(encodedMessage);
		icmpv6Option_SLLAO.setField("optionType", optionType);
		System.out.println("Finish to decode optionType");
		
		System.out.println("Start to decode length_SLLAO");
		IntegerValue length_SLLAO = decodeNumber(encodedMessage);
		icmpv6Option_SLLAO.setField("length_SLLAO", length_SLLAO);
		System.out.println("Finish to decode length_SLLAO");
		
		System.out.println("Start to decode srcLinkAddress");
		if(length_SLLAO.getInt()*64-16>=0){
			HexstringValue srcLinkAddress = super.createHexstringValue(encodedMessage, length_SLLAO.getInt()*64-16);
			icmpv6Option_SLLAO.setField("srcLinkAddress", srcLinkAddress);
			System.out.println("Finish to decode srcLinkAddress");
		}
		
	}
	
	private void decodeIcmpv6Option_MTU(byte[] encodedMessage,
			RecordValue icmpv6Option_MTU) {
		System.out.println("Start to decode optionType");
		IntegerValue optionType = decodeNumber(encodedMessage);
		icmpv6Option_MTU.setField("optionType", optionType);
		System.out.println("Finish to decode optionType");
		
		System.out.println("Start to decode length_MTU");
		IntegerValue length_MTU = decodeNumber(encodedMessage);
		icmpv6Option_MTU.setField("length_MTU", length_MTU);
		System.out.println("Finish to decode length_MTU");
		
		System.out.println("Start to decode mtu");
		IntegerValue mtu = decodeNumber4(encodedMessage);
		icmpv6Option_MTU.setField("mtu", mtu);
		System.out.println("Finish to decode mtu");
	}


	private void decodeLowpan_NHC(byte[] encodedMessage, RecordValue lowpan_NHC) {
		System.out.println("<-----开始解码fixedValue_Fragment");
		BitstringValue fixedValue_Fragment = super.createBitstringValue(encodedMessage, 4);
		//System.out.println("***************"+fixedValue_Fragment.getString());
		lowpan_NHC.setField("fixedValue_Fragment", fixedValue_Fragment);
		System.out.println("<----结束解码fixedValue_Fragment");
		
		System.out.println("<-----开始解码cid_Fragment");
		BitstringValue cid_Fragment = super.createBitstringValue(encodedMessage, 3);
		if("'010'B".equals(cid_Fragment.getString())){   //扩展选项为 分片选项
			hasFragmentOption = true;
		}
		lowpan_NHC.setField("cid_Fragment", cid_Fragment);
		System.out.println("<----结束解码cid_Fragment");
		
		System.out.println("<-----开始解码nh_Fragment");
		BitstringValue nh_Fragment = super.createBitstringValue(encodedMessage, 1);
		lowpan_NHC.setField("nh_Fragment", nh_Fragment);
		System.out.println("***************"+nh_Fragment.getString());
		System.out.println("<----结束解码nh_Fragment");		
	}

	private void decodeFragmentOption(byte[] encodedMessage,
			RecordValue fragmentOption) {
		
		
		System.out.println("<-----开始解码length_Fragment");
		IntegerValue length_Fragment = decodeNumber(encodedMessage);
		fragmentOption.setField("length_Fragment", length_Fragment);
		System.out.println("***************"+length_Fragment.getInt());
		System.out.println("<----结束解码length_Fragment");
		
		
		System.out.println("<-----开始解码nextHeader_Fragment");
		IntegerValue nextHeader_Fragment = decodeNumber(encodedMessage);
		fragmentOption.setField("nextHeader_Fragment", nextHeader_Fragment);
		System.out.println("***************"+nextHeader_Fragment.getInt());
		System.out.println("<----结束解码nextHeader_Fragment");
		
		System.out.println("<-----开始解码reserve_a");
		BitstringValue reserve_a = super.createBitstringValue(encodedMessage, 8);
		fragmentOption.setField("reserve_a", reserve_a);
		System.out.println("<----结束解码reserve_a");
		
		System.out.println("<-----开始解码offset");
		BitstringValue offset = super.createBitstringValue(encodedMessage, 13);
		fragmentOption.setField("offset", offset);
		System.out.println("<----结束解码offset");
		
		System.out.println("<-----开始解码reserve_b");
		BitstringValue reserve_b = super.createBitstringValue(encodedMessage, 2);
		fragmentOption.setField("reserve_b", reserve_b);
		System.out.println("<----结束解码reserve_b");
		
		System.out.println("<-----开始解码m_Fragment");
		BitstringValue m_Fragment = super.createBitstringValue(encodedMessage, 1);
		fragmentOption.setField("m_Fragment", m_Fragment);
		System.out.println("<----结束解码m_Fragment");
		
		System.out.println("<-----开始解码identification");
		BitstringValue identification = super.createBitstringValue(encodedMessage, 32);
		fragmentOption.setField("identification", identification);
		System.out.println("<----结束解码identification");
	}

	private void decodeLowpanHeader(byte[] encodedMessage,
			RecordValue result) {
		System.out.println("Start to decode fixedValue");
		BitstringValue fixedValue = super.createBitstringValue(encodedMessage, 3);
		//System.out.println(fixedValue.getString());
		result.setField("fixedValue", fixedValue);
		System.out.println("Finish to decode fixedValue");
		
		
		System.out.println("Start to decode tf");
		BitstringValue tf = super.createBitstringValue(encodedMessage, 2);
		//System.out.println(tf.getString());
		//判断IPv6首部的流量类型和流标签是否省略
		if("'11'B".equals(tf)){  //省略
			hasTF = false;   //流量类型和流标签省略
		}			
		result.setField("tf", tf);
		System.out.println("Finish to decode tf");
		
		
		System.out.println("Start to decode nh");
		BitstringValue nh = super.createBitstringValue(encodedMessage, 1);
		//System.out.println(nh.getString());
		//判断IPv6首部的下一首部字段是否省略
		if("'0'B".equals(nh.getString())){  //未省略
			hasNextHead = true;
		}			
		result.setField("nh", nh);
		System.out.println("Finish to decode nh");
		
		
		System.out.println("Start to decode hlim");
		BitstringValue hlim = super.createBitstringValue(encodedMessage, 2);
		//判断IPv6首部的HLIM字段是否省略
		if("'11'B".equals(hlim.getString())){  //省略
			hasHLIM = false;
			HLIM="'11'B";
		}else if("'01'B".equals(hlim.getString())){  //省略
			hasHLIM = false;
			HLIM="'01'B";
		}else if("'10'B".equals(hlim.getString())){  //省略
			hasHLIM = false;
			HLIM="'10'B";
		}	
		result.setField("hlim", hlim);
		System.out.println("Finish to decode hlim");
		
		System.out.println("Start to decode cid");
		BitstringValue cid = super.createBitstringValue(encodedMessage, 1);
		result.setField("cid", cid);		
		System.out.println("Finish to decode cid");
		
		
		System.out.println("Start to decode sac");
		BitstringValue sac = super.createBitstringValue(encodedMessage, 1);
		SAC=sac.getString();
		result.setField("sac", sac);
		System.out.println("Finish to decode sac");
		
		
		System.out.println("Start to decode sam");
		BitstringValue sam = super.createBitstringValue(encodedMessage, 2);
		SAM=sam.getString();
		result.setField("sam",sam);
		System.out.println("Finish to decode sam");
		
		
		System.out.println("Start to decode m");
		BitstringValue m = super.createBitstringValue(encodedMessage, 1);
		M=m.getString();
		result.setField("m", m);
		System.out.println("Finish to decode m");
		
		
		System.out.println("Start to decode dac");
		BitstringValue dac = super.createBitstringValue(encodedMessage, 1);
		DAC=dac.getString();
		result.setField("dac", dac);
		System.out.println("Finish to decode dac");
		
		
		System.out.println("Start to decode dam");
		BitstringValue dam = super.createBitstringValue(encodedMessage, 2);
		DAM=dam.getString();
		result.setField("dam", dam);
		//System.out.println(dam.getString());
		System.out.println("Finish to decode dam");
		
	}

	private void decodeICMPV6RequestOrReplyMessage(byte[] encodedMessage,
			RecordValue result) {
		System.out.println("Start to decode packetType");
		IntegerValue packetType = decodeNumber(encodedMessage);
		//System.out.println(packetType.getInt());
		result.setField("packetType", packetType);
		System.out.println("Finish to decode packetType");
		
		
		System.out.println("Start to decode code");
		BitstringValue code = super.createBitstringValue(encodedMessage, 8);
		result.setField("code", code);
		System.out.println("Finish to decode code");
		
		
		System.out.println("Start to decode validateSum");
		BitstringValue validateSum = super.createBitstringValue(encodedMessage, 16);
		result.setField("validateSum", validateSum);
		System.out.println("Finish to decode validateSum");
		
		
		System.out.println("Start to decode identifier");
		BitstringValue identifier = super.createBitstringValue(encodedMessage, 16);
		result.setField("identifier", identifier);
		System.out.println("Finish to decode identifier");
		
		
		System.out.println("Start to decode serialNumber");
		BitstringValue serialNumber = super.createBitstringValue(encodedMessage, 16);
		result.setField("serialNumber", serialNumber);
		System.out.println("Finish to decode serialNumber");
		
		
		System.out.println("Start to decode icmpv6Data");
		
		//计算ICMPv6的数据部分长度,单位为比特
		int len = datagram_size*8-bitpos;
		
		HexstringValue icmpv6Data = super.createHexstringValue(encodedMessage, len);
		result.setField("icmpv6Data", icmpv6Data);
		System.out.println("Finish to decode icmpv6Data");
	}

	private void decodeIPV6Header(byte[] encodedMessage, RecordValue result) {
		// TODO Auto-generated method stub
		if(isNoCompressIPv6){   //如果有ipv6Version
			System.out.println("<----开始解码ipv6Version");
			BitstringValue ipv6Version = super.createBitstringValue(encodedMessage, 4);
			result.setField("ipv6Version", ipv6Version);
			System.out.println("<----结束解码ipv6Version");
		}
		
		if(hasTF){   //如果有trafficType和flow
			System.out.println("<----开始解码trafficType");
			BitstringValue trafficType = super.createBitstringValue(encodedMessage, 8);
			result.setField("trafficType", trafficType);
			System.out.println("<----结束解码trafficType");
			
			System.out.println("<----开始解码flow");
			BitstringValue flow = super.createBitstringValue(encodedMessage, 20);
			result.setField("flow", flow);
			System.out.println("<----结束解码flow");
		}
		
		if(isNoCompressIPv6){   //如果有length_Load
			System.out.println("<----开始解码length_Load");
			IntegerValue length_Load = decodeNumber2(encodedMessage);
			result.setField("length_Load", length_Load);
			System.out.println("<----结束解码length_Load");
		}
		
		
		if(hasNextHead){ //如果有下一首部
			System.out.println("<----开始解码nextHeader");
			IntegerValue nextHeader = decodeNumber(encodedMessage);
			//System.out.println(nextHeader.getInt());
			result.setField("nextHeader", nextHeader);
			System.out.println("<----结束解码nextHeader");
		}
		
		if(hasHLIM){
			System.out.println("<----开始解码hopLimit");
			IntegerValue hopLimit = decodeNumber(encodedMessage);
			result.setField("hopLimit", hopLimit);
			//System.out.println(hopLimit.getInt());
			System.out.println("<----结束解码hopLimit");
		}
		if("'0'B".equals(SAC)){
			if("'00'B".equals(SAM)){    //sac=0,sam=00    128bit
				System.out.println("<----开始解码sourceAddress");
				HexstringValue sourceAddress = super.createHexstringValue(encodedMessage, 128);
				//System.out.println(sourceAddress.getString());
				result.setField("sourceAddress", sourceAddress); 
				System.out.println("<----结束解码sourceAddress");
			}else if("'01'B".equals(SAM)){   //sac=0,sam=01    64bit
				System.out.println("<----开始解码sourceAddress");
				HexstringValue sourceAddress = super.createHexstringValue(encodedMessage, 64);
				//System.out.println(sourceAddress.getString());
				result.setField("sourceAddress", sourceAddress); 
				System.out.println("<----结束解码sourceAddress");
			}else if("'10'B".equals(SAM)){   //sac=0,sam=10    16bit
				System.out.println("<----开始解码sourceAddress");
				HexstringValue sourceAddress = super.createHexstringValue(encodedMessage, 16);
				//System.out.println(sourceAddress.getString());
				result.setField("sourceAddress", sourceAddress); 
				System.out.println("<----结束解码sourceAddress");
			}else if("'11'B".equals(SAM)){   //sac=0,sam=11    0bit
				System.out.println("<----开始解码sourceAddress");
				System.out.println("全部省略");
				System.out.println("<----结束解码sourceAddress");
			}
		}else if("'1'B".equals(SAC)){
			if("'00'B".equals(SAM)){    //sac=1,sam=00                未指定
				System.out.println("<----开始解码sourceAddress");
				System.out.println("未指定");
				System.out.println("<----结束解码sourceAddress");
			}else if("'01'B".equals(SAM)){   //sac=1,sam=01    64bit
				System.out.println("<----开始解码sourceAddress");
				HexstringValue sourceAddress = super.createHexstringValue(encodedMessage, 64);
				//System.out.println(sourceAddress.getString());
				result.setField("sourceAddress", sourceAddress); 
				System.out.println("<----结束解码sourceAddress");
			}else if("'10'B".equals(SAM)){   //sac=1,sam=10    16bit
				System.out.println("<----开始解码sourceAddress");
				HexstringValue sourceAddress = super.createHexstringValue(encodedMessage, 16);
				//System.out.println(sourceAddress.getString());
				result.setField("sourceAddress", sourceAddress); 
				System.out.println("<----结束解码sourceAddress");
			}else if("'11'B".equals(SAM)){   //sac=1,sam=11    0bit
				System.out.println("<----开始解码sourceAddress");
				System.out.println("全部省略");
				System.out.println("<----结束解码sourceAddress");
			}
		}
		
		if("'0'B".equals(M)){
			if("'0'B".equals(DAC)){
				if("'00'B".equals(DAM)){    //M=0，DAC=0, DAM=00     128bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 128);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'01'B".equals(DAM)){   //M=0，DAC=0, DAM=01     64bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 64);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'10'B".equals(DAM)){   //M=0，DAC=0, DAM=10     16bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 16);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'11'B".equals(DAM)){   //M=0，DAC=0, DAM=11     0bit
					System.out.println("<----开始解码destinationAddress");
					System.out.println("全部省略");
					System.out.println("<----结束解码destinationAddress");
				}
			}else if("'1'B".equals(DAC)){
				if("'00'B".equals(DAM)){    //M=0，DAC=1, DAM=00     保留
					System.out.println("<----开始解码destinationAddress");
					System.out.println("保留");
					System.out.println("<----结束解码destinationAddress");
				}else if("'01'B".equals(DAM)){   //M=0，DAC=1, DAM=01     64bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 64);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'10'B".equals(DAM)){   //M=0，DAC=1, DAM=10     16bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 16);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'11'B".equals(DAM)){   //M=0，DAC=1, DAM=11     0bit
					System.out.println("<----开始解码destinationAddress");
					System.out.println("全部省略");
					System.out.println("<----结束解码destinationAddress");
				}
			}
		}else if("'1'B".equals(M)){
			if("'0'B".equals(DAC)){
				if("'00'B".equals(DAM)){    //M=1，DAC=0, DAM=00        128bit
					System.out.println("<----开始解码destinationAddress");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 128);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					System.out.println("<----结束解码destinationAddress");
				}else if("'01'B".equals(DAM)){   //M=1，DAC=0, DAM=01     48bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 48);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'10'B".equals(DAM)){   //M=1，DAC=0, DAM=10     32bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 32);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else if("'11'B".equals(DAM)){   //M=1，DAC=0, DAM=11     8bit
					System.out.println("<----开始解码destinationAddress");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 8);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					System.out.println("<----结束解码destinationAddress");
				}
			}else if("'1'B".equals(DAC)){
				if("'00'B".equals(DAM)){   //M=1，DAC=1, DAM=00     48bit
					System.out.println("<----开始解码destinationAddress");
					//System.out.println("********1");
					HexstringValue destinationAddress = super.createHexstringValue(encodedMessage, 48);
					//System.out.println("********");
					result.setField("destinationAddress", destinationAddress);
					//System.out.println(destinationAddress.getString());
					System.out.println("<----结束解码destinationAddress");
				}else{   //M=1，DAC=1, DAM=01 10 11     保留
					System.out.println("<----开始解码destinationAddress");
					System.out.println("全部省略");
					System.out.println("<----结束解码destinationAddress");
				}
			}
		}
		
	}

	

	private IntegerValue decodeNumber(byte[] encodedMessage) {
		// TODO Auto-generated method stub
		BitstringValue number = super.createBitstringValue(encodedMessage, 8);
		int num = 0;
		for(int i=0; i<number.getLength(); i++)
		{
			num|=number.getBit(i)<<(7-i);
		}
		IntegerValue num1 = (IntegerValue)RB.getTciCDRequired().getInteger().newInstance();
		num1.setInt(num);
		return num1;
	}

	//解析出16比特
	private IntegerValue decodeNumber2(byte[] encodedMessage) {
		BitstringValue number = super.createBitstringValue(encodedMessage, 16);
		int num = 0;
		for(int i=0; i<number.getLength(); i++)
		{
			num|=number.getBit(i)<<(15-i);
		}
		IntegerValue num1 = (IntegerValue)RB.getTciCDRequired().getInteger().newInstance();
		num1.setInt(num);
		return num1;
	}
	
	//解析出32比特
	private IntegerValue decodeNumber3(byte[] encodedMessage) {
		BitstringValue number = super.createBitstringValue(encodedMessage, 32);
		int num = 0;
		for(int i=0; i<number.getLength(); i++)
		{
			num|=number.getBit(i)<<(31-i);
		}
		IntegerValue num1 = (IntegerValue)RB.getTciCDRequired().getInteger().newInstance();
		num1.setInt(num);
		return num1;
	}
	
	//解析出48比特
	private IntegerValue decodeNumber4(byte[] encodedMessage) {
		BitstringValue number = super.createBitstringValue(encodedMessage, 48);
		int num = 0;
		for(int i=0; i<number.getLength(); i++)
		{
			num|=number.getBit(i)<<(47-i);
		}
		IntegerValue num1 = (IntegerValue)RB.getTciCDRequired().getInteger().newInstance();
		num1.setInt(num);
		return num1;
	}

}
