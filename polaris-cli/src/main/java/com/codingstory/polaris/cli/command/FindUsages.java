package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.CommandUtils;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.parser.TTypeUsage;
import com.codingstory.polaris.parser.TypeUsage;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TGetTypeRequest;
import com.codingstory.polaris.search.TGetTypeResponse;
import com.codingstory.polaris.search.TListTypeUsagesRequest;
import com.codingstory.polaris.search.TListTypeUsagesResponse;
import org.apache.thrift.TException;

import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.checkStatus;
import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "find-usages")
public class FindUsages {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Option(name = "server", shortName = "s")
    public String server;

    @Run
    public void run(String[] args) throws TException, IOException {
        if (args.length != 1) {
            die("Require exactly one class");
        }

        final String className = args[0];
        CommandUtils.openIndexOrConnectToServerAndRun(index, server, new CommandUtils.RpcRunner() {
            @Override
            public void run(TCodeSearchService.Iface rpc) throws TException {
                TGetTypeRequest getTypeReq = new TGetTypeRequest();
                getTypeReq.setTypeName(className);
                TGetTypeResponse getTypeResp = rpc.getType(getTypeReq);
                checkStatus(getTypeResp.getStatus());
                TListTypeUsagesRequest listUsageReq = new TListTypeUsagesRequest();
                listUsageReq.setTypeId(getTypeResp.getClassType().getHandle().getId());
                TListTypeUsagesResponse listUsageResp = rpc.listTypeUsages(listUsageReq);
                checkStatus(listUsageResp.getStatus());
                for (TTypeUsage t : listUsageResp.getUsages()) {
                    TypeUsage usage = TypeUsage.createFromThrift(t);
                    System.out.println(usage.getKind().name() + ": file #" + usage.getJumpTarget().getFileId() +
                            ":" + usage.getJumpTarget().getSpan().getFrom().getLine());
                }
            }
        });
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris find-usages <qualified-class> [--index=<dir>] [--server=<ip:port>]\\n" +
                "\n" +
                "Options:\n" +
                HelpMessages.INDEX +
                HelpMessages.SERVER +
                "\n");
    }
}
