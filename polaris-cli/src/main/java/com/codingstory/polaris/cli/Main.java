package com.codingstory.polaris.cli;

import com.codingstory.polaris.cli.command.Search;
import com.codingstory.polaris.cli.command.SearchServer;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static com.codingstory.polaris.cli.CommandUtils.die;

public class Main {

    private static final List<Class<?>> COMMANDS = ImmutableList.<Class<?>>of(
            Search.class, SearchServer.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        boolean help = false;
        String command = args[0];
        if (Objects.equal(command, "help")) {
            if (args.length == 1) {
                printHelp();
                return;
            }
            help = true;
            command = args[1];
        }
        Class<?> clazz = getCommandClass(command);
        if (clazz == null) {
            die("Unknown command: " + command);
        }
        try {
            Object instance = clazz.newInstance();
            if (help) {
                Method m = getAnnotatedMethod(clazz, Help.class);
                m.invoke(instance);
                return;
            }
            Method m = getAnnotatedMethod(clazz, Run.class);
            Options options = new Options();
            List<Field> optionFields = Lists.newArrayList();
            for (Field field : clazz.getFields()) {
                if (field.getAnnotation(Option.class) != null) {
                    optionFields.add(field);
                }
            }
            for (Field field : optionFields) {
                Option option = field.getAnnotation(Option.class);
                boolean hasArg;
                hasArg = !isBooleanField(field);
                options.addOption(
                        Strings.isNullOrEmpty(option.shortName()) ? null : option.shortName(),
                        option.name(),
                        hasArg,
                        null);
            }
            CommandLineParser commandLineParser = new GnuParser();
            CommandLine commandLine = commandLineParser.parse(
                    options, Arrays.copyOfRange(args, 1, args.length));
            for (Field field : optionFields) {
                Option option = field.getAnnotation(Option.class);
                boolean appear = commandLine.hasOption(option.name());
                if (appear && isRequired(option)) {
                    die("Missing required option: " + option.name());
                }
                if (isBooleanField(field)) {
                    field.setBoolean(instance, appear);
                } else {
                    String value = commandLine.getOptionValue(option.name());
                    if (value == null) {
                        value = option.defaultValue();
                    }
                    field.set(instance, value);
                }
            }
            m.invoke(instance, new Object[] { commandLine.getArgs() });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printHelp() {
        System.out.print("Usage:\n" +
                "  polaris <command> <argument>..\n" +
                "\n" +
                "Available commands:\n");
        for (Class<?> clazz : COMMANDS) {
            System.out.println("  " + clazz.getAnnotation(Command.class).name());
        }
        System.out.print("  help\n\n");
    }

    private static Class<?> getCommandClass(String s) {
        for (Class<?> clazz : COMMANDS) {
            String name = clazz.getAnnotation(Command.class).name();
            if (Objects.equal(name, s)) {
                return clazz;
            }
        }
        return null;
    }

    private static Method getAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(annotation) != null) {
                return method;
            }
        }
        return null;
    }

    private static boolean isBooleanField(Field field) {
        Class<?> type = field.getType();
        return type.isPrimitive() && Objects.equal(type, Boolean.class);
    }

    private static boolean isRequired(Option option) {
        return !Strings.isNullOrEmpty(option.defaultValue());
    }
}
