/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.signer.azure;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.SingleTransactionSignerProvider;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Charsets;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = AzureSubCommand.COMMAND_NAME,
    description = "Sign transactions using the Azure signing service.",
    mixinStandardHelpOptions = true)
public class AzureSubCommand extends SignerSubCommand {

  @Option(
      names = {"--keyvault-name", "--key-vault-name"},
      description = "Name of the vault to access - used as the sub-domain to vault.azure.net",
      required = true,
      arity = "1")
  private String keyVaultName;

  @Option(
      names = {"--key-name"},
      description = "The name of the key which is to be used",
      required = true)
  private String keyName;

  @Option(
      names = {"--key-version"},
      description = "The version of the requested key to use",
      required = true)
  private String keyVersion;

  @Option(
      names = {"--client-id"},
      description = "The ID used to authenticate with Azure key vault",
      required = true)
  private String clientId;

  @Option(
      names = {"--client-secret-path"},
      description =
          "Path to a file containing the secret used to access the vault (along with client-id)",
      required = true)
  private Path clientSecretPath;

  private static final String READ_SECRET_FILE_ERROR = "Error when reading the secret from file.";
  public static final String COMMAND_NAME = "azure-signer";

  private TransactionSigner createSigner() throws TransactionSignerInitializationException {
    final String clientSecret;
    try {
      clientSecret = readSecretFromFile(clientSecretPath);
    } catch (final IOException e) {
      throw new TransactionSignerInitializationException(READ_SECRET_FILE_ERROR, e);
    }

    final AzureConfig config =
        new AzureConfig(keyVaultName, keyName, keyVersion, clientId, clientSecret);

    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory(new AzureKeyVaultAuthenticator());

    return factory.createSigner(config);
  }

  @Override
  public TransactionSignerProvider createSignerFactory()
      throws TransactionSignerInitializationException {
    return new SingleTransactionSignerProvider(createSigner());
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  private static String readSecretFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}
