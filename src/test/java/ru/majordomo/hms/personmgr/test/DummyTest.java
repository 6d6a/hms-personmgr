package ru.majordomo.hms.personmgr.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.ArrayList;
import java.util.Collection;

public class DummyTest {

    @Test
    public void stupidOne() throws Exception {
//        Collection<UnixAccount> unixAccounts = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String json = "[{\"id\":\"58ff723f9daec20007c42770\",\"name\":\"u2068\",\"switchedOn\":true,\"uid\":2068,\"homeDir\":\"/home/u2068\",\"serverId\":\"5821f8c596ccde0001c82a61\",\"quota\":10485760,\"quotaUsed\":24576,\"writable\":true,\"sendmailAllowed\":true,\"passwordHash\":\"\",\"keyPair\":{\"privateKey\":\"-----BEGIN RSA PRIVATE KEY-----\\nMIIEowIBAAKCAQEAqJPHPJvkfuE+vROm4K7zTy8YP6rMu62YydPsoiwM7KTQTe6b\\nhghOwuiHOy8IItokWrCN6Q5tC4iOW9gVwNdtyRDXeXjm3dxGMsfs9XKPyV7SiFH4\\n0G80KQzHBedFmn+NZjWDqP6ZQ4O2ECjbx5u/ZB+v/c+WH77/7shtpeGNVSDjE8mY\\ngjw4/p4dfoZEtB9JbH8Y1MZDNQ7RDpwVOi6gpFdMyiJuasL6uXCdFXm8iTSdbZFI\\nbBOXvn7x9ojWS3Jb0ioxf08oKlDTQybLI1vyxNs7FvtZA77jwFXCVyBxnQFbfHhU\\n3rEFIG0U10exLMK48BRELzW5vpWrGS8mLhbR/QIDAQABAoIBAFG8/dEMgblnvATv\\n3214Ru9xFV/hkE27+aZ7BHUJyOaBb6Mp++z8YDwvhUqUHmzPuuriSpzjaso511T/\\nLGUJz+i7Ks9yaPbQVJVQzTuh5cgtGwYQQQXHtdHlqSbaoawtBsG1VvZ9JJFQ7tld\\nBlv9z8pbdvkpS1BvSydbtZbGC8JnWDEn/st/1H1mFb2frAYU9pRclaVGyexblCIV\\nrRJ0dbtl8uS7MBpL7/zuwy0uA80vQhfGcCrUPO/544QZlCVzy0+U3qq4YRllMbVc\\nzhjWnsv7Z3nFLMD73F9Rmtv68pGEx4ekETNIrVyccFC6KD93XxyMVpcUjk5wqCs1\\nMIZ+UV0CgYEA+A5GXndBcKuBqSfTCR4FdmIkPacOYWW8k1guIvmfaW+6GEeUUNej\\ndZK1NRgH3fkxoq5xPa9+RsHxkAKuDV5y1j8Pfdk1TCi5S27BdVYr0FnTmM9etAXd\\nlVc+06ltHDcVS6l3qD+soV+JvHaDOnqgvOVNEe6YvpumYJd+H6xMNFcCgYEArfni\\n1mBag4PrLpU4c7MH1GY5z1AhcKkAkF8P6evL5qtfSpQNa4WjBroxlLO6xQHBfSas\\nra+iP5NTCT0JLZ6GE82Chv0nwfh4UfxqJBcGW7Vg2ht9wMasqiLjd+DA+/zBI2fD\\nNQyQx2GmZJ87H207TOLZJv6duiO5eqrU0i1Gl8sCgYBjJrGSCPErbCKDzttYBZwB\\nWfFKg4AIBnZ+Hv0yFb1Wk8Y69kGentNDp8Su2FAstfLVKA0zrvY2qkc4XRndVido\\n5AP5Nd1L9+s476h8klkpCv69UgZ0xvNvQlRmno5yfMISYEboBaunRVz07BpAQcwG\\nNVG9HTYI2oaCjYtodaJwOQKBgHkWiYlH+ve6Y4BCn833d6WfmPDwBrZRCnFxmLPp\\nSDu+X06bCVQi6TsPcN7c0Uu5UyB2QI2KxtSVB6BQeQjlYtB5ozWtEXTyKVx24LFJ\\nzUUbUtO7eqUoIpkwOz3+kpNQcmTgHX/YxRPgpV03boELF9EFurpnUQRzRO9Z41J9\\n7LpNAoGBAJqD1CJ11JRNVgFopuxmAuFqElr+Ox+uyhQiwM6tYELCdm7rLwKDQ0GZ\\n5zazZFBOq00GQxhjrnVEcXBVsHTphByw77Zd7ggzo/m3+/B+X2EnaHHLn+HA95Up\\n4x5rutXJNAQkckWxeutO4Hf7U7MwIvRT0bXP1Y/jG7HFSOemwHe8\\n-----END RSA PRIVATE KEY-----\\n\",\"publicKey\":\"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCok8c8m+R+4T69E6bgrvNPLxg/qsy7rZjJ0+yiLAzspNBN7puGCE7C6Ic7Lwgi2iRasI3pDm0LiI5b2BXA123JENd5eObd3EYyx+z1co/JXtKIUfjQbzQpDMcF50Waf41mNYOo/plDg7YQKNvHm79kH6/9z5Yfvv/uyG2l4Y1VIOMTyZiCPDj+nh1+hkS0H0lsfxjUxkM1DtEOnBU6LqCkV0zKIm5qwvq5cJ0VebyJNJ1tkUhsE5e+fvH2iNZLclvSKjF/TygqUNNDJssjW/LE2zsW+1kDvuPAVcJXIHGdAVt8eFTesQUgbRTXR7EswrjwFEQvNbm+lasZLyYuFtH9 \\n\"},\"crontab\":[]}]";
        UnixAccount unixAccount = mapper.readValue(json, UnixAccount.class);
        System.out.println(unixAccount);
    }
}
