package org.suai.generator.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.List;

@Data
@AllArgsConstructor
public class SendingParams {
    private String senderGroup;
    private String recipientGroup;
    private List<File> files;
}
