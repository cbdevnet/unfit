#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <endian.h>

int usage(char* fn){
	printf("Miband firmware splitter\n");
	printf("Usage: %s <firmare file>\n", fn);
	return -1;
}

void dump(char* file, uint8_t* buffer, size_t len){
	FILE* out = NULL;

	printf("\t\tDumping %d bytes to %s\n", len, file);
	out = fopen(file, "w");
	if(!out){
		printf("Failed to open %s for writing\n", file);
	}
	else{
		fwrite(buffer, 1, len, out);
		fclose(out);
	}
}

int main(int argc, char** argv){
	FILE* in = NULL;

	char* infile = NULL;
	char* outfile_hdr = "header.fw";
	char* outfile_a = "part1.fw";
	char* outfile_b = "part2.fw";
	char* outfile_trl = "trailer.fw";

	uint8_t* buffer = NULL;
	size_t filesize = 0, bytes = 0;
	int i;
	uint32_t* data;
	uint32_t data_end, data_begin;

	for(i = 1; i < argc; i++){
		if(!infile){
			infile = argv[i];
			printf("Using input file %s\n", infile);
		}
	}

	if(!infile){
		exit(usage(argv[0]));
	}

	in = fopen(infile, "r");
	if(!in){
		printf("Failed to open input file\n");
		exit(usage(argv[0]));
	}

	fseek(in, 0, SEEK_END);
	filesize = ftell(in);
	printf("Reading %d bytes of firmware\n", filesize);
	rewind(in);

	buffer = calloc(filesize, sizeof(uint8_t));
	if(!buffer){
		printf("Failed to allocate buffer for firmware data\n");
		exit(usage(argv[0]));
	}

	bytes = fread(buffer, 1, filesize, in);
	if(bytes == filesize){
		printf("[7]\t\tFirmware type(?): %s\t\t%02X\n", (buffer[7] == 1) ? "dual firmware (Band + HR)":"single firmware (Band only)", buffer[7]);

		printf("[ 8  9 10 11]\tFirmware 1 version: %d.%d.%d.%d\t\t\t\t%02X%02X%02X%02X\n", buffer[8], buffer[9], buffer[10], buffer[11], buffer[8], buffer[9], buffer[10], buffer[11]);

		data = (uint32_t*) (buffer + 12);
		printf("[12 13 14 15]\tFirmware 1 offset: %d\t\t\t\t\t%02X%02X%02X%02X\n", be32toh(*data), buffer[12], buffer[13], buffer[14], buffer[15]);
		data_begin = be32toh(*data);
		dump(outfile_hdr, buffer, data_begin);

		data = (uint32_t*) (buffer + 16);
		printf("[16 17 18 19]\tFirmware 1 length: %d\t\t\t\t%02X%02X%02X%02X\n", be32toh(*data), buffer[16], buffer[17], buffer[18], buffer[19]);
		data_end = data_begin + be32toh(*data);
		dump(outfile_a, buffer + data_begin, be32toh(*data));
		
		printf("\t\tFirmware 1 data end: %d\n", data_end);

		if(buffer[7] == 1){
			printf("[22 23 24 25]\tFirmware 2 version: %d.%d.%d.%d\t\t\t\t%02X%02X%02X%02X\n", buffer[22], buffer[23], buffer[24], buffer[25], buffer[22], buffer[23], buffer[24], buffer[25]);

			data = (uint32_t*) (buffer + 26);
			printf("[26 27 28 29]\tFirmware 2 offset: %d\t\t\t\t%02X%02X%02X%02X\n", be32toh(*data), buffer[26], buffer[27], buffer[28], buffer[29]);
			data_begin = be32toh(*data);

			data = (uint32_t*) (buffer + 30);
			printf("[30 31 32 33]\tFirmware 2 length: %d\t\t\t\t%02X%02X%02X%02X\n", be32toh(*data), buffer[30], buffer[31], buffer[32], buffer[33]);
			data_end = data_begin + be32toh(*data);
			dump(outfile_b, buffer + data_begin, be32toh(*data));
			
			printf("\t\tFirmware 2 data end: %d\n", data_end);
		}

		printf("\t\tTrailer length: %d\n", filesize - data_end);
		if(filesize - data_end){
			dump(outfile_trl, buffer + data_end, filesize - data_end);
		}
	}
	else{
		printf("Only %d bytes read from file, aborting\n", bytes);
	}

	fclose(in);
	free(buffer);
	return 0;
}
