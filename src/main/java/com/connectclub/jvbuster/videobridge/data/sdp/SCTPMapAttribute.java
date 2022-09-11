package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SCTPMapAttribute extends Attribute {

	private Integer number;
	private String app;
	private Integer streams;

	@Override
	public String getField() {
		return "sctpmap";
	}

	@Override
	public String getValue() {

		String value =  number + " " + app;
		
		if (streams!=null)
			value += " " + streams;
		
		return value;
	}
	
}
