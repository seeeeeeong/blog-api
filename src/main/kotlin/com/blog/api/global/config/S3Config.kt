package com.blog.api.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    private val awsProperties: AwsProperties
) {
    
    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            awsProperties.accessKey,
            awsProperties.secretKey
        )
        
        return S3Client.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
    
    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(
            awsProperties.accessKey,
            awsProperties.secretKey
        )
        
        return S3Presigner.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}
