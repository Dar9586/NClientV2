package com.dar.nclientv2.components;

import android.content.Context;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

import androidx.annotation.NonNull;

public class MySenderFactory implements ReportSenderFactory{
    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration config){
        return new MySender();
    }

    @Override
    public boolean enabled(@NonNull CoreConfiguration config){
        return true;
    }
}
