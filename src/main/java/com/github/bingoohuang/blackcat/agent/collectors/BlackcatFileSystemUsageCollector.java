package com.github.bingoohuang.blackcat.agent.collectors;


import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatFileSystemUsage.Usage;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

public class BlackcatFileSystemUsageCollector implements BlackcatCollector {
    @Override
    public Optional<BlackcatReq> collect() throws SigarException {
        /*
        tps：该设备每秒的传输次数（Indicate the number of transfers per second
        that were issued to the device.）。“一次传输”意思是“一次I/O请求”。
        多个逻辑请求可能会被合并为“一次I/O请求”。“一次传输”请求的大小是未知的。
        kB_read/s：每秒从设备（drive expressed）读取的数据量；
        kB_wrtn/s：每秒向设备（drive expressed）写入的数据量；
        kB_read：读取的总数据量；
        kB_wrtn：写入的总数量数据量；
        这些单位都为Kilobytes。

        Filesystem      Mounted on           Reads     Writes R-bytes W-bytes Queue Svctm
        /dev/disk1      /                        0          0      0       0      -     -
         */
        SigarProxy sigar = SigarFactory.newSigar();

        BlackcatMsg.BlackcatFileSystemUsage.Builder builder;
        builder = BlackcatMsg.BlackcatFileSystemUsage.newBuilder();

        FileSystem[] fileSystems = sigar.getFileSystemList();
        for (FileSystem fileSystem : fileSystems) {
            if (fileSystem.getType() != FileSystem.TYPE_LOCAL_DISK) continue;

            String dirName = fileSystem.getDirName();
            FileSystemUsage fsUsage = sigar.getFileSystemUsage(dirName);
            // https://searchcode.com/codesearch/view/8192367/

            Usage.Builder usageBuilder;
            usageBuilder = Usage.newBuilder();
            usageBuilder.setDevName(fileSystem.getDevName())
                    .setDirName(fileSystem.getDirName())
                    .setDiskReads(fsUsage.getDiskReads())
                    .setDiskWrites(fsUsage.getDiskWrites())
                    .setDiskReadBytes(fsUsage.getDiskReadBytes())
                    .setDiskWriteBytes(fsUsage.getDiskWriteBytes())
                    .setDiskQueue(fsUsage.getDiskQueue())
                    .setDiskServiceTime(fsUsage.getDiskServiceTime())
                    .setAvail(fsUsage.getAvail())
                    .setFree(fsUsage.getFree())
                    .setFiles(fsUsage.getFiles())
                    .setFreeFiles(fsUsage.getFreeFiles())
                    .setUsed(fsUsage.getUsed())
                    .setTotal(fsUsage.getTotal())
                    .setUsePercent(fsUsage.getUsePercent());

            builder.addUsage(usageBuilder);
        }

        BlackcatReq blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatFileSystemUsage))
                .setBlackcatFileSystemUsage(builder).build();
        return Optional.of(blackcatReq);
    }

    public static void main(String[] args) throws SigarException {
        new BlackcatFileSystemUsageCollector().collect();
    }
}
